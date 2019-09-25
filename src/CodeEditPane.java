import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;

/**
 *  Simple Code Editing Text Pane with Undo and Redo, Line Numbers and optional Breakpoint controls
 *  Author: Wayne Holder, 2017-2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

class CodeEditPane extends JPanel {
  private static final int      TAB_SIZE = 4;
  private JEditorPane           codePane;
  private static final Font     font = getCodeFont(12);
  private boolean               editable, breakpoints, lineNumbers;
  private LineNumbersView       lineView;
  private LineHighlightEditorKit kit;

  static class LineHighlightEditorKit extends StyledEditorKit implements ViewFactory {
    private JEditorPane editorPane;
    private int         lineNum;
    private Color       highlightColor;

    LineHighlightEditorKit (Color highlightColor) {
      this.highlightColor = highlightColor;
    }

    public ViewFactory getViewFactory () {
      return this;
    }

    public View create (Element elem) {
      String kind = elem.getName();
      if (kind != null) {
        switch (kind) {
          case AbstractDocument.ContentElementName:
            return new LabelView(elem);
          case AbstractDocument.ParagraphElementName:
            return new HighlightParagraphView(elem);
          case AbstractDocument.SectionElementName:
            return new BoxView(elem, View.Y_AXIS);
          case StyleConstants.ComponentElementName:
            return new ComponentView(elem);
          case StyleConstants.IconElementName:
            return new IconView(elem);
        }
      }
      return new LabelView(elem);
    }

    @Override
    public void install(JEditorPane editorPane) {
      this.editorPane = editorPane;
    }

    void highlightLine (int lineNum) {
      this.lineNum = lineNum;
      if (editorPane != null) {
        editorPane.repaint();
      }
    }

    class HighlightParagraphView extends ParagraphView {
      HighlightParagraphView (Element e) {
        super(e);
      }

      public void paintChild (Graphics g, Rectangle rect, int n) {
        Rectangle clip = g.getClipBounds();
        int lineIndex = getLineIndex();
        if (lineNum > 0 && lineIndex == lineNum - 1) {
          g.setColor(highlightColor);
          g.fillRect(rect.x, rect.y, clip.width, rect.height);
        }
        super.paintChild(g, rect, n);
      }

      private int getLineIndex () {
        int lineCount = 0;
        View parent = this.getParent();
        int count = parent.getViewCount();
        for (int i = 0; i < count; i++) {
          if (parent.getView(i) == this) {
            break;
          } else {
            lineCount += parent.getView(i).getViewCount();
          }
        }
        return lineCount;
      }
    }
  }

  /**
   * Implements left margin line number component for a JEditorPane in a JScrollPane.
   * Uses a separate component for the RowHeaderView of the JScrollPane. Pads the
   * line numbers up to 999 and handles wrapped lines and resizing of the editor.
   * Adapted from code copyrighted by rememberjava.com and Licensed under GPL 3.
   * See http://rememberjava.com/license
   */
  static class LineNumbersView extends JComponent {
    private static final int          MARGIN_WIDTH_PX = 26;
    private static final int          BREAK_WIDTH_PX = 15;
    private JEditorPane               editor;
    private List<Rectangle>           hits = new ArrayList<>();
    private int                       hitBase;
    private BitSet                    breakpoints;
    private List<BreakpointListener>  breakListeners = new ArrayList<>();

    interface BreakpointListener {
      void breakpointChanged (int line, boolean value);
    }

    LineNumbersView (JEditorPane editor, boolean useBreakpoints) {
      this.editor = editor;
      if (useBreakpoints) {
        breakpoints = new BitSet();
      }
      editor.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate (DocumentEvent e) {
          SwingUtilities.invokeLater(() -> repaint());
        }

        @Override
        public void removeUpdate (DocumentEvent e) {
          SwingUtilities.invokeLater(() -> repaint());
        }

        @Override
        public void changedUpdate (DocumentEvent e) {
          SwingUtilities.invokeLater(() -> repaint());
        }
      });
      editor.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized (ComponentEvent e) {
          updateSize();
          SwingUtilities.invokeLater(() -> repaint());
        }

        @Override
        public void componentShown (ComponentEvent e) {
          updateSize();
          SwingUtilities.invokeLater(() -> repaint());
        }
      });
      if (breakpoints != null) {
        addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed (MouseEvent ev) {
            Point loc = new Point(ev.getX(), ev.getY());
            for (int ii = 0; ii < hits.size(); ii++) {
              if (hits.get(ii).contains(loc)) {
                int idx = hitBase + ii;
                breakpoints.flip(idx);
                SwingUtilities.invokeLater(() -> repaint());
                for (BreakpointListener breakListener : breakListeners) {
                  breakListener.breakpointChanged(idx + 1, breakpoints.get(idx));
                }
                //int rowCount = editor.getDocument().getDefaultRootElement().getElementCount();
                // Need code here to breakpoint bits > rowCount
              }
            }
          }
        });
      }
    }

    void addBreakpointListener (BreakpointListener breakListener) {
      if (breakpoints != null) {
        synchronized (this) {
          breakListeners.add(breakListener);
        }
      } else {
        throw new IllegalStateException("Call to addBreakpointListener() when breakpoints are not enabled");
      }
    }

    void setBreakpoint (int line, boolean set) {
      if (breakpoints != null) {
        breakpoints.set(line - 1, set);
        SwingUtilities.invokeLater(this::repaint);
      } else {
        throw new IllegalStateException("Call to setBreakpoint() when breakpoints are not enabled");
      }
    }

    /**
     * Updates the size of the line number margin based on the editor height.
     */
    private void updateSize () {
      Dimension size = new Dimension(MARGIN_WIDTH_PX + (breakpoints != null ? BREAK_WIDTH_PX : 0), editor.getHeight());
      setPreferredSize(size);
      setSize(size);
      if (breakpoints != null) {
        // Clear any breakpoints that are now outside size of document
        int rowCount = editor.getDocument().getDefaultRootElement().getElementCount();
        while ((rowCount = breakpoints.nextSetBit(rowCount)) >= 0) {
          breakpoints.clear(rowCount);
        }
      }
    }

    @Override
    public void paintComponent (Graphics g) {
      super.paintComponent(g);
      List<Rectangle> hits = breakpoints != null ? new ArrayList<>() : null;
      Rectangle clip = g.getClipBounds();
      g.setColor(new Color(235, 235, 235));
      g.fillRect(clip.x, clip.y, clip.width, clip.height);
      int startOffset = editor.viewToModel(new Point(0, clip.y));
      int endOffset = editor.viewToModel(new Point(0, clip.y + clip.height));
      int x = getInsets().left + 2;
      Element root = editor.getDocument().getDefaultRootElement();
      FontMetrics fontMetrics = editor.getFontMetrics(editor.getFont());
      int descent = fontMetrics.getDescent();
      g.setFont(new Font(editor.getFont().getName(), Font.PLAIN, editor.getFont().getSize() - 2));
      int base = root.getElementIndex(startOffset);
      while (startOffset <= endOffset) {
        try {
          // Gets the line number String of the element based on startOffset, or null for wrapped lines).
          int index = root.getElementIndex(startOffset);
          Element line = root.getElement(index);
          String lineNumber =  line.getStartOffset() == startOffset ? String.format("%3d", index + 1) : null;
          if (lineNumber != null) {
            // Computes the y axis position for the line number belonging to the element at startOffset
            Rectangle rect = editor.modelToView(startOffset);
            int y = rect.y + rect.height - descent;
            g.setColor(Color.darkGray);
            g.drawString(lineNumber, x, y);
            if (breakpoints != null && hits != null) {
              hits.add(new Rectangle(x + MARGIN_WIDTH_PX, rect.y + 3, BREAK_WIDTH_PX, rect.height - 4));
              if (false) {
                g.setColor(new Color(220, 220, 220));
                g.fillRect(x + MARGIN_WIDTH_PX, rect.y + 3, BREAK_WIDTH_PX, rect.height - 4);
              }
              if (breakpoints.get(index)) {
                g.setColor(Color.red);
                g.fillOval(x + MARGIN_WIDTH_PX + 2, rect.y + 4, 8, 8);
              } else {
                g.setColor(new Color(200, 200, 200));
                g.drawOval(x + MARGIN_WIDTH_PX + 2, rect.y + 4, 8, 8);
              }
            }
          }
          startOffset = Utilities.getRowEnd(editor, startOffset) + 1;
        } catch (BadLocationException e) {
          e.printStackTrace();
        }
      }
      if (breakpoints != null) {
        synchronized (this) {
          this.hits = hits;
          hitBase = base;
        }
      }
    }
  }

  /*
   ********************** CodeEditPane **********************
   */

  void addBreakpointListener (LineNumbersView.BreakpointListener breakpointListener) {
    if (lineView != null && breakpoints && lineNumbers) {
      lineView.addBreakpointListener( breakpointListener);
    }
  }

  void highlightLine (int lineNum) {
    if (kit != null) {
      kit.highlightLine(lineNum);
    }
  }

  /**
   * Implements a Code Editing Text Pane with Undo and Redo, Line Numbers and optional Breakpoint controls
   * @param title Title String for titled border around text, or null if no titled border
   * @param lineNumbers if true, display line numbers in left column, else none
   * @param breakpoints if true and lineNumbers is also true, adds controls for setting and clearing breakpoints
   * @param editable if true, JEditorPane is editable, else not editable
   */
  CodeEditPane (String title, boolean lineNumbers, boolean breakpoints, boolean editable) {
    this.lineNumbers = lineNumbers;
    this.breakpoints = breakpoints;
    this.editable = editable;
    setLayout(new BorderLayout());
    codePane = new JEditorPane();
    codePane.setContentType("text/cpp");
    codePane.setFont(font);
    Border border;
    if (title != null) {
      Border outside = BorderFactory.createTitledBorder(title);
      Border inside = BorderFactory.createEmptyBorder(5, 5, 5, 5);
      border = BorderFactory.createCompoundBorder(outside, inside);
    } else {
      border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    }
    codePane.setBorder(border);
    if (breakpoints) {
      kit = new LineHighlightEditorKit(new Color(191, 196, 255));
      codePane.setEditorKit(kit);
    } else {
      codePane.setEditorKit(new StyledEditorKit());
    }
    JScrollPane codeScrollpane = new JScrollPane(codePane);
    if (lineNumbers) {
      lineView = new LineNumbersView(codePane, breakpoints);
      codeScrollpane.setRowHeaderView(lineView);
    }
    add(codeScrollpane, BorderLayout.CENTER);
    StyledDocument doc = (StyledDocument) codePane.getDocument();
    int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    UndoManager undoManager = new UndoManager();
    doc.addUndoableEditListener(undoManager);
    // Map undo action
    KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, cmdMask);
    codePane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(undoKeyStroke, "undoKeyStroke");
    codePane.getActionMap().put("undoKeyStroke", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ev) {
        try {
          undoManager.undo();
        } catch (CannotUndoException ex) {
          // ignore
        }
      }
    });
    // Map redo action
    KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, cmdMask + InputEvent.SHIFT_MASK);
    codePane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(redoKeyStroke, "redoKeyStroke");
    codePane.getActionMap().put("redoKeyStroke", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ev) {
        try {
          undoManager.redo();
        } catch (CannotRedoException ex) {
          // ignore
        }
      }
    });
    // Setup tabs for StyledDocument
    BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
    FontMetrics fm = img.getGraphics().getFontMetrics(font);
    int charWidth = fm.charWidth('w');
    int tabWidth = charWidth * TAB_SIZE;
    TabStop[] tabs = new TabStop[35];
    for (int j = 0; j < tabs.length; j++) {
      int tab = j + 1;
      tabs[j] = new TabStop( tab * tabWidth );
    }
    TabSet tabSet = new TabSet(tabs);
    SimpleAttributeSet attributes = new SimpleAttributeSet();
    StyleConstants.setTabSet(attributes, tabSet);
    int length = doc.getLength();
    doc.setParagraphAttributes(0, length, attributes, false);
    codePane.updateUI();
    if (lineView != null) {
      lineView.updateSize();
    }
    codePane.setEditable(editable);
  }

  static Font getCodeFont (int points) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return new Font("Consolas", Font.PLAIN, points);
    } else if (os.contains("mac")) {
      return new Font("Menlo", Font.PLAIN, points);
    } else if (os.contains("linux")) {
      return new Font("Courier", Font.PLAIN, points);
    } else {
      return new Font("Courier", Font.PLAIN, points);
    }
  }

  void appendText (String line) {
    Document doc = codePane.getDocument();
    try {
      doc.insertString(doc.getLength(), line, null);
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }
  }

  String getText () {
    return codePane.getText();
  }

  void setText (String text) {
    if (editable) {
      // Convert all leading spaces to tabs
      StringBuilder buf = new StringBuilder();
      String[] parts = text.split("\n");
      for (int ii = 0; ii < parts.length; ii++) {
        String line = parts[ii];
        if (line.length() > 0) {
          int tabCount = 0;
          for (int jj = 0; jj < line.length(); jj++) {
            char cc = line.charAt(jj);
            if (cc == ' ') {
              if (++tabCount == TAB_SIZE) {
                buf.append("\t");
                tabCount = 0;
              }
            } else if (cc == '\t') {
              buf.append(cc);
            } else {
              buf.append(line.substring(jj).trim());
              if (ii < parts.length - 1) {
                buf.append("\n");
              }
              break;
            }
          }
        } else {
          buf.append("\n");
        }
      }
      codePane.setText(buf.toString());
    } else {
      codePane.setText(text);
    }
  }
}

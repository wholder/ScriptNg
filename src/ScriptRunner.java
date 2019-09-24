
/*
 * ScriptRunner implements a simple GUI for editing and running my experimental ScriptNg interpreter
 * that supports the following features:
 *
 *    1. Run ScriptNg scripts at full speed, or vary the speed to watch the code execute line by line
 *    3. Use "STEP" button to manually advance line by line through program
 *    2. Set breakpoints by clicking the controls next to the line numbers
 *    4. After hitting a breakpoint you can press "RUN" to resume running, or "STEP" to advance one line
 *    5. Press "STOP" to pause running code, then use "STEP" or "RUN", or press "STOP" again to cancel execution
 *    6. Read and Save scripts using the File menu, or select "New" to clear the edit pane.
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
 */

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class ScriptRunner extends JFrame {
  private transient Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
  private CodeEditPane  output;
  private CodeEditPane  code;
  private BitSet        breakpoints = new BitSet();
  private int           runDelay;
  private boolean       running;
  private RunState      runState = RunState.Run;
  private JButton       runButton = new JButton("RUN");
  private JButton       stepButton = new JButton("STEP");
  private JButton       stopButton = new JButton("STOP");
  private AtomicBoolean stopPressed = new AtomicBoolean(false);
  private AtomicBoolean stepPressed = new AtomicBoolean(false);
  private AtomicBoolean runPressed = new AtomicBoolean(false);

  private void appendText (String text) {
    SwingUtilities.invokeLater(() -> output.appendText(text));
  }

  enum RunState {
    Run, Step
  }

  private ScriptRunner () throws IOException {
    super("Script Runner");
    setLayout(new BorderLayout());
    output = new CodeEditPane("Output", false, false, false);
    code = new CodeEditPane("Script", true, true, true);
    code.addBreakpointListener((line, value) -> breakpoints.set(line, value));
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, output, code);
    splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    add(splitPane, BorderLayout.CENTER);
    splitPane.setDividerLocation(240);
    // Add MenuBar
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    // Add "File" Menu
    JMenu fileMenu = new JMenu("File");
    // Add "New" Menu item
    JMenuItem newItem = new JMenuItem("New Script");
    newItem.addActionListener(ev -> code.setText(""));
    fileMenu.add(newItem);
    // Add "Open ScriptNg File" menu item
    JMenuItem open = new JMenuItem("Read Script");
    open.addActionListener(ev -> {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Open a ScriptNg File");
      fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      FileNameExtensionFilter nameFilter = new FileNameExtensionFilter("ScriptNg files (*.script)", "script");
      fileChooser.addChoosableFileFilter(nameFilter);
      fileChooser.setFileFilter(nameFilter);
      fileChooser.setSelectedFile(new File(prefs.get("default.dir", "/")));
      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
          File file = fileChooser.getSelectedFile();
          prefs.put("default.dir", file.getAbsolutePath());
          InputStream fis = new FileInputStream(file);
          byte[] data = new byte[fis.available()];
          fis.read(data);
          fis.close();
          code.setText(new String(data, StandardCharsets.UTF_8));
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(this, "Unable to load file", "Error", JOptionPane.PLAIN_MESSAGE);
          ex.printStackTrace(System.out);
        }
      }
    });
    fileMenu.add(open);
    // Add "Save ScriptNg File" menu item
    JMenuItem save = new JMenuItem("Save Script");
    save.addActionListener(e -> {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save a ScriptNg File");
      FileNameExtensionFilter nameFilter = new FileNameExtensionFilter("ScriptNg files (*.script)", "script");
      fileChooser.addChoosableFileFilter(nameFilter);
      fileChooser.setFileFilter(nameFilter);
      fileChooser.setSelectedFile(new File(prefs.get("default.dir", "example.script")));
      if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if (!file.exists() || doWarningDialog("Overwrite Existing file?")) {
          saveFile(file, code.getText());
        }
        prefs.put("default.dir", file.getAbsolutePath());
      }
    });
    fileMenu.add(save);
    // Add file menu to menuBar
    menuBar.add(fileMenu);
    // Add "Run Speed" menu
    JMenu speedMenu = new JMenu("Run: Full Speed");
    ButtonGroup speedGroup = new ButtonGroup();
    for (String label : new String[] {"Full Speed:0", "4 lines/sec:250", "2 lines/sec:500", "1 line/sec:1000"}) {
      String[] parts = label.split(":");
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(parts[0]);
      speedGroup.add(item);
      speedMenu.add(item);
      menuBar.add(speedMenu);
      item.addActionListener(ev -> {
        runDelay = Integer.parseInt(parts[1]);
        speedMenu.setText("Run: " + parts[0]);
      });
    }
    // Setup Control Buttons
    stopButton.setEnabled(false);
    stopButton.addActionListener(ev -> stopPressed.set(true));
    JPanel controls = new JPanel(new GridLayout(1, 3));
    controls.add(runButton);
    controls.add(stepButton);
    controls.add(stopButton);
    add(controls, BorderLayout.SOUTH);
    runButton.addActionListener(ev -> {
      if (running) {
        runPressed.set(true);
      } else {
        try {
          runState = RunState.Run;
          new Thread(this::runCode).start();
        } catch (Exception ex) {
          appendText(ex.getMessage() + "\n");
        }
      }
    });
    stepButton.addActionListener(ev -> {
      if (running) {
        stepPressed.set(true);
      } else {
        try {
          runState = RunState.Step;
          new Thread(this::runCode).start();
        } catch (Exception ex) {
          appendText(ex.getMessage() + "\n");
        }
      }
    });
    code.setText(getFile("res:loop.script"));
    setSize(600, 800);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setLocation(prefs.getInt("window.x", 10), prefs.getInt("window.y", 10));
    // Track window resize/move events and save in prefs
    addComponentListener(new ComponentAdapter() {
      public void componentMoved (ComponentEvent ev) {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.x", bounds.x);
        prefs.putInt("window.y", bounds.y);
      }
    });
    setVisible(true);
  }

  private void runCode () {
    running = true;
    String script = code.getText();
    Map<String, ExpressionParser.Function> funcs = new HashMap<>();
    funcs.put("print", new Print(false));
    funcs.put("println", new Print(true));
    output.setText("");
    new Thread(() -> {
      runButton.setEnabled(false);
      runPressed.set(false);
      stepButton.setEnabled(false);
      stepPressed.set(false);
      stopButton.setEnabled(true);
      stopPressed.set(false);
      ScriptNg runner = new ScriptNg(script, funcs);
      stopPressed.set(false);
      Object ret = runner.run(lineNum -> {
        boolean clearHighlight = false;
        if ((runState == RunState.Run && breakpoints.get(lineNum)) || runState == RunState.Step) {
          code.highlightLine(lineNum);
          clearHighlight = true;
          runButton.setEnabled(true);
          stepButton.setEnabled(true);
          while (true) {
            if (stopPressed.get()) {
              break;
            } else if (stepPressed.get()) {
              runState = RunState.Step;
              stepPressed.set(false);
              break;
            } else if (runPressed.get()) {
              runState = RunState.Run;
              runPressed.set(false);
              break;
            }
            wait(10);
          }
          runButton.setEnabled(false);
          stepButton.setEnabled(false);
        } else  if (runDelay > 0) {
          code.highlightLine(lineNum);
          clearHighlight = true;
        }
        int delayCount = runState == RunState.Run ? runDelay / 10 : 0;
        do {
          if (stopPressed.get()) {
            if (runState == RunState.Run) {
              runState = RunState.Step;
              stopPressed.set(false);
            } else {
              throw new ScriptNg.StoppedException();
            }
          }
          if (delayCount-- > 0) {
            wait(10);
          }
        } while (delayCount > 0);
        if (clearHighlight) {
          code.highlightLine(0);
        }
      });
      if (ret != null) {
        if (ret instanceof ScriptNg.StoppedException) {
          appendText("\nStopped");
        } else {
          appendText("Returned: " + ret.toString());
        }
      }
      runButton.setEnabled(true);
      stepButton.setEnabled(true);
      stopButton.setEnabled(false);
      code.highlightLine(0);
      running = false;
    }).start();
  }

  private void wait (int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }

  // Implement function for print() and println()
  class Print implements ExpressionParser.Function {
    private boolean addLf;

    Print (boolean addLf) {
      this.addLf = addLf;
    }

    public Object call (LinkedList<Object> stack) {
      Object arg = stack.removeLast();
      appendText(arg.toString());
      if (addLf) {
        appendText("\n");
      }
      return null;
    }
  }

  private boolean doWarningDialog (String question) {
    ImageIcon icon = new ImageIcon(ScriptNg.class.getResource("warning-32x32.png"));
    return JOptionPane.showConfirmDialog(this, question, "Warning", JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE, icon) == JOptionPane.OK_OPTION;
  }

  static void saveFile (File file, String text) {
    try {
      FileOutputStream out = new FileOutputStream(file);
      out.write(text.getBytes(StandardCharsets.UTF_8));
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static String getFile (String file) throws IOException {
    InputStream fis;
    if (file.startsWith("res:")) {
      fis = ScriptNg.class.getClassLoader().getResourceAsStream(file.substring(4));
    } else {
      fis = new FileInputStream(file);
    }
    if (fis != null) {
      byte[] data = new byte[fis.available()];
      fis.read(data);
      fis.close();
      return new String(data, StandardCharsets.UTF_8);
    }
    throw new IllegalStateException("getFile() " + file + " not found");
  }

  public static void main (String[] args) throws IOException {
    new ScriptRunner();
  }
}

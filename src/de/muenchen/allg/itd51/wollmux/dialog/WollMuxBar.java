/*
* Dateiname: WollMuxBar.java
* Projekt  : WollMux
* Funktion : Men�-Leiste als zentraler Ausgangspunkt f�r WollMux-Funktionen
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 02.01.2006 | BNK | Erstellung
* 03.01.2006 | BNK | Men�s unterst�tzt
* 10.01.2006 | BNK | Icon und Config-File pfadunabh�ngig �ber Classloader
*                  | switches --minimize, --topbar, --normalwindow
* 06.02.2006 | BNK | Men�leiste hinzugef�gt
* 14.02.2006 | BNK | Minimieren r�ckg�ngig machen bei Aktivierung der Leiste.
* 15.02.2006 | BNK | ordentliches Abort auch bei schliessen des Icon-Fensters
* 19.04.2006 | BNK | [R1342][R1398]gro�e Aufr�umaktion, Umstellung auf WollMuxBarEventHandler
* 20.04.2006 | BNK | [R1207][R1205]Icon der WollMuxBar konfigurierbar, Anzeigemodus konfigurierbar
* 21.04.2006 | BNK | Umgestellt auf UIElementFactory
*                  | Bitte Warten... in der Senderbox solange noch keine Verbindung besteht
*                  | Wenn ein Men� mehrfach verwendet wird, so wird jetzt jedes
*                  | Mal ein neues erzeugt, um Probleme zu vermeiden, die auftreten
*                  | k�nnten, wenn das selbe JMenu an mehreren Stellen in der
*                  | Komponentenhierarchie erscheint.
* 24.04.2006 | BNK | kleinere Aufr�umarbeiten. Code Review.
* 24.04.2006 | BNK | [R1390]Popup-Fenster, wenn Verbindung zu OOo WollMux nicht hergestellt
*                  | werden konnte.
* 24.04.2006 | BNK | [R1460]Popup-Fenster, wenn WollMux nicht konfiguriert.
* 02.05.2006 | BNK | [R1202 Teil 1] Fensterposition und Gr��e von WollMuxBar konfigurierbar
* 29.05.2006 | BNK | in initFactories() Label Typen explizit genullt.
*                  | Umstellung auf UIElementFactory.Context
* 16.06.2006 | BNK | Fokusverlust wird simuliert jedes Mal wenn der Benutzer was
*                  | dr�ckt, damit sich die WollMuxBar dann minimiert.
* 21.06.2006 | BNK | Gross/Kleinschreibung ignorieren beim Auswertden des MODE
*                  | Es wird jetzt der letzte Fenster/WollMuxBar-Abschnitt verwendet.
* 23.06.2006 | BNK | Senderbox von JComboBox auf JPopupMenu umgestellt.    
* 27.06.2006 | BNK | WIDTH, HEIGHT max korrekt unterst�tzt 
* 29.06.2006 | BNK | min, max, center unterst�tzt    
* 19.07.2006 | BNK | MODE "Icon" repariert 
* 02.08.2006 | BNK | bessere Fehlermeldung wenn Konfiguration nicht gefunden.    
* 19.10.2006 | BNK | +ACTION "kill" +ACTION "dumpInfo"    
* 25.10.2006 | BNK | [P923][R3585]F�r den minimierten Zustand wird kein extra Fenster mehr verwendet.
* 25.10.2006 | BNK | Icon-Mode entfernt.
* 26.10.2006 | LUT | +ACTION "about"
*                  | +getBuildInfo(), das die buildinfo-Datei der WollMuxBar.jar ausliest
* 15.01.2007 | BNK | --load hinzugefuegt
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import com.sun.star.document.MacroExecMode;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.DispatchHandler;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Men�-Leiste als zentraler Ausgangspunkt f�r WollMux-Funktionen.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBar
{
  /**
   * Titel des WollMuxBar-Fensters (falls nicht anders konfiguriert).
   */
  private static final String DEFAULT_TITLE = "Vorlagen und Formulare";
  
  /**
   * Spezialeintrag in der Absenderliste, 
   * der genau dann vorhanden ist, wenn die Absenderliste leer ist.
   */
  private static final String LEERE_LISTE = "<kein Absender vorhanden>";
  
  /**
   * Wenn die WollMuxBar den Fokus verliert, minimiert sich das Fenster.
   */
  private static final int MINIMIZE_TO_TASKBAR_MODE = 1;
  /**
   * Die WollMuxBar verh�lt sich wie ein normales Fenster. 
   */
  private static final int NORMAL_WINDOW_MODE = 2;
  /**
   * Die WollMuxBar ist immer im Vordergrund.
   */
  private static final int ALWAYS_ON_TOP_WINDOW_MODE = 3;
  /**
   * Die WollMuxBar verschwindet am oberen Rand, wenn der Mauscursor sie verl�sst.
   */
  private static final int UP_AND_AWAY_WINDOW_MODE = 4;
  
  /**
   * TODO Die WollMuxBar ist vertikal und verschwindet am linken Rand, wenn der Mauscursor sie verl�sst.
   */
  //private static final int LEFT_AND_AWAY_WINDOW_MODE = 5;
  
  /**
   * Der Anzeigemodus f�r die WollMuxBar (z,B, {@link #UP_AND_AWAY_WINDOW_MODE}).
   */
  private int windowMode;
  
  /**
   * Dient der thread-safen Kommunikation mit dem entfernten WollMux.
   */
  private WollMuxBarEventHandler eventHandler;
  
  /**
   * Der Rahmen, der die Steuerelemente enth�lt.
   */
  private JFrame myFrame;
  
  /**
   * Falls > 0, so ist dies eine von wollmux,conf fest vorgegebene Breite.
   * Falls 0, so wird die nat�rliche Breite verwendet.
   * Falls -1, so wird die maximale Breite verwendet.
   */
  private int myFrame_width;
  
  /**
   * Falls > 0, so ist dies eine von wollmux,conf fest vorgegebene H�he.
   * Falls 0, so wird die nat�rliche H�he verwendet.
   * Falls -1, so wird die maximale H�he verwendet.
   */
  private int myFrame_height;
  
  /**
   * Falls >= 0, so ist dies eine von wollmux,conf fest vorgegebene x-Koordinate.
   * Diese wird nur einmal gesetzt. Danach kann der Benutzer das Fenster verschieben,
   * wenn er m�chte.
   * Falls -1, so wird das Fenster zentriert.
   * Falls -2, so wird die gr��te sinnvolle Koordinate verwendet.
   * Falls -3, so wird die kleinste sinnvolle Koordinate verwendet.
   * Falls Integer.MIN_VALUE, so ist keine Koordinate fest vorgegeben.
   */
  private int myFrame_x;
  
  /**
   * Falls >= 0, so ist dies eine von wollmux,conf fest vorgegebene y-Koordinate.
   * Diese wird nur einmal gesetzt. Danach kann der Benutzer das Fenster verschieben,
   * wenn er m�chte.
   * Falls -1, so wird das Fenster zentriert.
   * Falls -2, so wird die gr��te sinnvolle Koordinate verwendet.
   * Falls -3, so wird die kleinste sinnvolle Koordinate verwendet.
   * Falls Integer.MIN_VALUE, so ist keine Koordinate fest vorgegeben.
   */
  private int myFrame_y;
  
  /**
   * Das Panel f�r den Inhalt des Fensters der WollMuxBar (myFrame).
   */
  private JPanel contentPanel;
  
  /**
   * Mappt einen Men�-Namen auf ein entsprechendes JPopupMenu.
   */
  private Map mapMenuNameToJPopupMenu = new HashMap();
  
  /**
   * Die UIElementFactory, die verwendet wird, um das GUI aufzubauen.
   */
  private UIElementFactory uiElementFactory;
  
  /**
   * Kontext f�r GUI-Elemente in JPanels (f�r �bergabe an die uiElementFactory).
   */
  private UIElementFactory.Context panelContext;
  
  /**
   * Kontext f�r GUI-Elemente in JMenus und JPopupMenus (f�r �bergabe an die uiElementFactory).
   */
  private UIElementFactory.Context menuContext;
  
  /**
   * Rand um Textfelder (wird auch f�r ein paar andere R�nder verwendet)
   * in Pixeln.
   */
  private final static int TF_BORDER = 4;
  
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * Die Fehlermeldung die in einem Popup-Fenster gebracht wird, wenn keine
   * Verbindung zum WollMux in OOo hergestellt werden konnte.
   */
  private static final String CONNECTION_FAILED_MESSAGE = 
  "Es konnte keine Verbindung zur WollMux-Komponente von OpenOffice hergestellt werden.\n"+
  "Eine m�gliche Ursache ist ein fehlerhaft installiertes OpenOffice.\n"+
  "Eine weitere m�gliche Ursache ist, dass WollMux.uno.pkg nicht oder fehlerhaft "+
  "installiert wurde.";

  private static final String WOLLMUX_CONFIG_ERROR_MESSAGE = 
  "Aus Ihrer WollMux-Konfiguration konnte kein Abschnitt \"Symbolleisten\" gelesen werden.\n"+
  "Die WollMux-Leiste kann daher nicht gestartet werden. Bitte �berpr�fen Sie, ob in Ihrer wollmux.conf\n"+
  "der %include f�r die Konfiguration der WollMuxBar (z.B. wollmuxbar_standard.conf) vorhanden ist und\n"+
  "�berpr�fen Sie anhand der wollmux.log ob evtl. beim Verarbeiten eines %includes ein Fehler\n"+
  "aufgetreten ist.";
  
  /**
   * ActionListener f�r Buttons mit der ACTION "abort". 
   */
  private ActionListener actionListener_abort = new ActionListener()
       { public void actionPerformed(ActionEvent e){ abort(); } };
    
  /**
   * ActionListener f�r Buttons, denen ein Men� zugeordnet ist. 
   */
  private ActionListener actionListener_openMenu = new ActionListener()
        { public void actionPerformed(ActionEvent e){ openMenu(e); } };
  
  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;
  
  /**
   * Aufgerufen wenn der Spezialeintrag "Liste Bearbeiten" in der Senderbox
   * gew�hlt wird.
   */
  private ActionListener actionListener_editSenderList = new ActionListener() 
  { public void actionPerformed(ActionEvent e) { 
    eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmPALVerwalten, null); 
    minimize(); } }; 
  
  /**
   * ActionListener wenn anderer Absender in Senderbox ausgew�hlt. 
   */
  private ActionListener senderboxActionListener = new ActionListener() 
    { public void actionPerformed(ActionEvent e) { senderBoxItemChanged(e); } };

  /**
   * �berwacht, ob sich die Maus in irgendwo innerhalb einer Komponente der
   * WollMuxBar befindet.
   */
  private IsInsideMonitor myIsInsideMonitor = new IsInsideMonitor();
    
  /**
   * Alle {@link Senderbox}es der Leiste.
   */
  private List senderboxes = new Vector();

  /**
   * Die breite der minimierten WollMux-Leiste im UP_AND_AWAY_WINDOW_MODE.
   */
  private int minimizedWidth = 300;
  
  /**
   * Wird im UP_AND_AWAY_WINDOW_MODE auf das Fenster registriert.
   */
  private UpAndAwayWindowTransformer upAndAwayWindowTransformer = new UpAndAwayWindowTransformer();

  /**
   * Das Panel, das das Aussehen des Strichs im UP_AND_AWAY_WINDOW_MODE bestimmt.
   */
  private JPanel upAndAwayMinimizedPanel;

   /**
   * Die Men�leiste der WollMuxBar.
   */
  private JMenuBar menuBar;
  
  /**
   * true zeigt an, dass die Leiste minimiert ist.
   */
  private boolean isMinimized = false;
  
  
  /**
   * Erzeugt eine neue WollMuxBar.
   * @param winMode Anzeigemodus, z.B. {@link #UP_AND_AWAY_WINDOW_MODE}.
   * @param conf der Inhalt der wollmux.conf 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(int winMode, final ConfigThingy conf)
  {
    windowMode = winMode;

    eventHandler = new WollMuxBarEventHandler(this);
    
    /*
     * Die GUI wird im Event-Dispatching Thread erzeugt wg. Thread-Safety.
     * Auch eventHandler.connectWithWollMux() wird im EDT ausgef�hrt, um
     * sicherzustellen, dass kein updateSenderBoxes() ausgef�hrt wird, bevor
     * nicht die Senderboxen erzeugt wurden.
     */
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{
              /*
               * Dieser Befehl steht VOR dem Aufruf von createGUI(), damit
               * OOo schon gestartet wird, w�hrend wir noch mit GUI aufbauen
               * besch�ftigt sind. Es ist trotztdem sichergestellt, dass
               * updateSenderboxes() nicht vor der Beendigung von createGUI()
               * aufgerufen werden kann, weil updateSenderboxes() durch den
               * WollMuxBarEventHandler ebenfalls mit invokeLater() in den EDT
               * geschickt wird und dort erst zum Zug kommen kann, wenn diese
               * run() Methode beendet ist. 
               */
              eventHandler.connectWithWollMux();
              
              createGUI(conf);
            }catch(Exception x)
            {
              Logger.error(x);
            };
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }

  private void createGUI(ConfigThingy conf)
  {
    initFactories();
    
    //Wohl nicht mehr erforderlich, seit auf ein einziges Fenster umgestellt wurde:
    //Mit file:///C:/Programme/j2sdk1.4.2_08/docs/api/java/awt/doc-files/FocusSpec.html  das Blink-Problem in Griff kriegen und vielleicht auch die WollMuxBar nicht mehr fokussierbar machen (vor allem die minimierte Version). Eventuell nuetzlich dazu sind JWindow-Klasse und evtl. muss ein blinder JFrame oder ein blindes JWindow als Parent in die Hierarchie eingefuegt werden (als Parent der eigentlichen WollMuxBar-Fenster)
   
    //Toolkit tk = Toolkit.getDefaultToolkit();
    //GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    //Dimension screenSize = tk.getScreenSize();
    //Rectangle bounds = genv.getMaximumWindowBounds();
    
    String title = DEFAULT_TITLE;
    ConfigThingy wmBarConf = new ConfigThingy("");
    try{
      wmBarConf = conf.query("Fenster").query("WollMuxBar").getLastChild(); 
    }catch(Exception x) {}
    try{title = wmBarConf.get("TITLE").toString();}catch(Exception x) {}
    
    myFrame_x = Integer.MIN_VALUE;
    try{
      String xStr = wmBarConf.get("X").toString();
      if (xStr.equalsIgnoreCase("center"))
        myFrame_x = -1;
      else if (xStr.equalsIgnoreCase("max"))
        myFrame_x = -2;
      else if (xStr.equalsIgnoreCase("min"))
        myFrame_x = -3;
      else
      {
        myFrame_x = Integer.parseInt(xStr);
          // Ja, das folgende ist eine Einschr�nkung, aber 
          // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
          // obigen Festlegungen
        if (myFrame_x < 0) myFrame_x = 0;
      }
    }catch(Exception x) {}
    
    myFrame_y = Integer.MIN_VALUE;
    try{
      String yStr = wmBarConf.get("Y").toString();
      if (yStr.equalsIgnoreCase("center"))
        myFrame_y = -1;
      else if (yStr.equalsIgnoreCase("max"))
        myFrame_y = -2;
      else if (yStr.equalsIgnoreCase("min"))
        myFrame_y = -3;
      else
      {
        myFrame_y = Integer.parseInt(yStr);
          // Ja, das folgende ist eine Einschr�nkung, aber 
          // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
          // obigen Festlegungen
        if (myFrame_y < 0) myFrame_y = 0;
      }
    }catch(Exception x) {}
    
    myFrame_width = 0;
    try{
      String widthStr = wmBarConf.get("WIDTH").toString();
      if (widthStr.equalsIgnoreCase("max"))
        myFrame_width = -1;
      else
      {
        myFrame_width = Integer.parseInt(widthStr);
        if (myFrame_width < 0) myFrame_width = 0;
      }
    }catch(Exception x) {}
    
    myFrame_height = 0;
    try{
      String heightStr = wmBarConf.get("HEIGHT").toString();
      if (heightStr.equalsIgnoreCase("max"))
        myFrame_height = -1;
      else
      {
        myFrame_height = Integer.parseInt(heightStr);
        if (myFrame_height < 0) myFrame_height = 0;
      }
    }catch(Exception x) {}
    
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    
    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.setUndecorated(true);
      //myFrame.setFocusable(false);
      //myFrame.setFocusableWindowState(false);
      myFrame_y = 0;
    }
    
    //Ein WindowListener, der auf den JFrame registriert wird, damit als
    //Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgef�hrt wird.
    myFrame.addWindowListener(new MyWindowListener());
    
    WindowTransformer myWindowTransformer = new WindowTransformer();
    myFrame.addWindowFocusListener(myWindowTransformer);
    
    contentPanel = new JPanel();
    contentPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    contentPanel.setLayout(new GridBagLayout());
    contentPanel.addMouseListener(myIsInsideMonitor);
    myFrame.getContentPane().add(contentPanel);
    
    try{
      ConfigThingy bkl = conf.query("Symbolleisten").query("Briefkopfleiste");
      if(bkl.count() > 0) {
        addUIElements(conf.query("Menues"),bkl.getLastChild(), contentPanel, 1, 0, "panel");
      }
    }catch(NodeNotFoundException x)
    {
      Logger.error(x);
    }
    
    menuBar = new JMenuBar();
    menuBar.addMouseListener(myIsInsideMonitor);
    try{
      ConfigThingy menubar = conf.query("Menueleiste");
      if(menubar.count() > 0) {
        addUIElements(conf.query("Menues"),menubar.getLastChild(), menuBar, 1, 0, "menu");
      }
    }catch(NodeNotFoundException x)
    {
      Logger.error(x);
    }
    myFrame.setJMenuBar(menuBar);
    
    setupMinimizedFrame(title, wmBarConf);

    if (windowMode != NORMAL_WINDOW_MODE) myFrame.setAlwaysOnTop(true);
    
    setSizeAndLocation();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Passt die Gr��e und Position der Fenster an.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setSizeAndLocation()
  {
    if (isMinimized) return;
    // Toolkit tk = Toolkit.getDefaultToolkit();
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    //Dimension screenSize = tk.getScreenSize();
    Rectangle bounds = genv.getMaximumWindowBounds();
    
    myFrame.pack();
    Dimension naturalFrameSize = myFrame.getSize();
    Dimension frameSize = new Dimension(naturalFrameSize);
    Point frameLocation = myFrame.getLocation(); 
    
    switch (myFrame_width)
    {
      case 0: // natural width
        break;
      case -1: // max
        frameSize.width = bounds.width;
        break;
      default: // specified width
        frameSize.width = myFrame_width;
        break;
    }
    
    switch (myFrame_height)
    {
      case 0: // natural height
        break;
      case -1: // max
        frameSize.height = bounds.height;
        break;
      default: // specified height
        frameSize.height = myFrame_height;
        break;
    }
    
    switch (myFrame_x)
    {
      case -1: // center
        frameLocation.x = bounds.x + (bounds.width-frameSize.width)/2;
        break;
      case -2: // max
        frameLocation.x = bounds.x + bounds.width - frameSize.width;
        break;
      case -3: // min
        frameLocation.x = bounds.x;
        break;
      case Integer.MIN_VALUE: // kein Wert angegeben
        break;
      default: // Wert angegeben, wird nur einmal ber�cksichtigt.
        frameLocation.x = myFrame_x;
        myFrame_x = Integer.MIN_VALUE;
        break;
    }
    
    switch (myFrame_y)
    {
      case -1: // center
        frameLocation.y = bounds.y + (bounds.height-frameSize.height)/2;
        break;
      case -2: // max
        frameLocation.y = bounds.y + bounds.height - frameSize.height;
        break;
      case -3: // min
        frameLocation.y = bounds.y;
        break;
      case Integer.MIN_VALUE: // kein Wert angegeben
        break;
      default: // Wert angegeben, wird nur einmal ber�cksichtigt.
        frameLocation.y = myFrame_y;
        myFrame_y = Integer.MIN_VALUE;
        break;
    }
    
    myFrame.setSize(frameSize);
    myFrame.setLocation(frameLocation);
    myFrame.validate(); //ohne diese wurde in Tests manchmal nicht neu gezeichnet
        
    minimizedWidth = frameSize.width;
    if (minimizedWidth > 128) minimizedWidth -= 64;
    
  }

  /**
   * Erzeugt den JFrame f�r die minimierte Darstellung (WollMux-Logo oder
   * schmaler Streifen).
   * @param title der Titel f�r das Fenster (nur f�r Anzeige in Taskleiste)
   * @param wmBarConf ConfigThingy des Fenster/WollMuxBar-Abschnitts.
   * @param upAndAwayWidth breite des Streifens f�r Modus "UpAndAway"
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setupMinimizedFrame(String title, ConfigThingy wmBarConf)
  {
    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
    {
      upAndAwayMinimizedPanel = new JPanel();
      upAndAwayMinimizedPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    }
  }
  
  /**
   * F�gt der Komponente compo UI Elemente hinzu, eines f�r jedes Kind von 
   * elementParent.
   * 
   * @param menuConf die Kinder dieses ConfigThingys m�ssen "Menues"-Knoten sein,
   *        deren Kinder Men�beschreibungen sind f�r die Men�s, 
   *        die als UI Elemente verwendet werden.
   * @param elementParent
   * @param context kann die Werte "menu" oder "panel" haben und gibt an, um was
   *        es sich bei compo handelt. Abh�ngig vom context werden manche 
   *        UI Elemente anders interpretiert, z.B. werden "button" Elemente im
   *        context "menu" zu JMenuItems.        
   * @param compo die Komponente zu der die UI Elemente hinzugef�gt werden sollen.
   *        Falls context nicht "menu" ist, muss compo ein GridBagLayout haben.
   * @param stepx stepx und stepy geben an, um wieviel mit jedem UI Element die x 
   *        und die y Koordinate innerhalb des GridBagLayouts erh�ht werden sollen.
   *        Sinnvoll sind hier normalerweise nur (0,1) und (1,0).
   * @param stepy siehe stepx
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addUIElements(ConfigThingy menuConf, ConfigThingy elementParent, 
      JComponent compo, int stepx, int stepy, String context)
  {
    addUIElementsChecked(new HashSet(), menuConf, elementParent, compo, stepx, stepy, context);
  }
  
  /**
   * Wie addUIElements, aber reicht den Parameter alreadySeen an parseMenu weiter,
   * um sich gegenseitig enthaltende Men�s zu erkennen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void addUIElementsChecked(Set alreadySeen, ConfigThingy menuConf, ConfigThingy elementParent, 
      JComponent compo, int stepx, int stepy, String context)
  {
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    //GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcMenuButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcSenderbox  = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
      
    int y = -stepy;
    int x = -stepx; 
      
    UIElementFactory.Context contextMap = context.equals("menu") ? menuContext : panelContext;
    
    Iterator piter = elementParent.iterator();
    while (piter.hasNext())
    {
      ConfigThingy uiElementDesc = (ConfigThingy)piter.next();
      y += stepy;
      x += stepx;
      
      try{
        String type;
        try{
          type = uiElementDesc.get("TYPE").toString();
        }
        catch(NodeNotFoundException e)
        {
          Logger.error("Ein User Interface Element ohne TYPE wurde entdeckt");
          continue;
        }
        
        if (type.equals("senderbox"))
        {
          char hotkey = 0;
          try{
            hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
          }catch(Exception e){}
          
          
          String label = "Bitte warten...";
          Senderbox senderbox;
          JComponent menu;
          AbstractButton button;
          if (context.equals("menu"))
          {
            menu = new JMenu(label);
            button = (AbstractButton)menu;
            senderbox = Senderbox.create((JMenu)menu);
          }
          else
          { 
            menu = new JPopupMenu();
            String menuName = "SenD3rB0x_"+Math.random();
            mapMenuNameToJPopupMenu.put(menuName, menu);
            button = new JButton(label);
            button.addActionListener(actionListener_openMenu) ;
            button.setActionCommand(menuName);
            button.setBackground(Color.WHITE);
            button.setFocusable(false);
            senderbox = Senderbox.create((JPopupMenu)menu, button);
          }
          
          button.setMnemonic(hotkey);
          
          senderboxes.add(senderbox);
          
          gbcMenuButton.gridx = x;
          gbcMenuButton.gridy = y;
          button.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(button);
          else
            compo.add(button, gbcSenderbox);
        }
        else if (type.equals("menu"))
        {
          String label = "LABEL FEHLT ODER FEHLERHAFT!";
          try{ label = uiElementDesc.get("LABEL").toString(); } catch(Exception e){}
          
          char hotkey = 0;
          try{
            hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
          }catch(Exception e){}
          
          String menuName = "";
          try{
            menuName = uiElementDesc.get("MENU").toString();
          }catch(NodeNotFoundException e){}
          
          AbstractButton button;
          if (context.equals("menu"))
          {
            button = (AbstractButton)parseMenu(alreadySeen, null, menuConf, menuName, new JMenu(label));
            if (button == null)
              button = new JMenu(label);
          }
          else
          { 
            parseMenu(alreadySeen, mapMenuNameToJPopupMenu, menuConf, menuName, new JPopupMenu());
            button = new JButton(label);
            button.addActionListener(actionListener_openMenu) ;
            button.setActionCommand(menuName);
          }
          
          button.setMnemonic(hotkey);
          
          gbcMenuButton.gridx = x;
          gbcMenuButton.gridy = y;
          button.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(button);
          else
            compo.add(button, gbcMenuButton);
        }
        else
        {
          UIElement uiElement = uiElementFactory.createUIElement(contextMap, uiElementDesc);
          GridBagConstraints gbc = (GridBagConstraints)uiElement.getLayoutConstraints();
          gbc.gridx = x;
          gbc.gridy = y;
          Component uiComponent = uiElement.getComponent();
          uiComponent.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(uiComponent);
          else
            compo.add(uiComponent, gbc);
        }
      }
      catch(ConfigurationErrorException e) {Logger.error(e);}
    }
  }
  
  /**
   * Parst eine Men�beschreibung und erzeugt ein entsprechendes Men�.
   * @param menu das JMenu oder JPopupMenu zu dem die UI Elemente hinzugef�gt 
   *        werden sollen.
   * @param menuConf die Kinder dieses ConfigThingys m�ssen "Menues"-Knoten sein,
   *        deren Kinder Men�beschreibungen sind. 
   * @param menuName identifiziert das Men� aus menuConf, das geparst wird. Gibt es
   *        mehrere, so wird das letzte verwendet.
   * @param mapMenuNameToMenu falls nicht-null, so wird falls bereits ein Eintrag
   *                          menuName enthalten ist, dieser zur�ckgeliefert, 
   *                          ansonsten wird ein Mapping von menuName auf menu
   *                          hinzugef�gt.
   *                          Falls null, so wird immer ein neues Men� erzeugt,
   *                          au�er das menuName ist in alreadySeen, dann gibt
   *                          es eine Fehlermeldung.
   * @param alreadySeen falls menuName hier enthalten ist und mapMenuNameToMenu==null
   *                    dann wird eine Fehlermeldung ausgegeben und null zur�ckgeliefert.
   *                          
   * @return menu, falls das Men� erfolgreich aufgebaut werden konnte, null, wenn 
   *         das Men� nicht in menuConf definiert ist oder wenn es in alreadySeen
   *         ist und mapMenuNameToMenu == null.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private JComponent parseMenu(Set alreadySeen, Map mapMenuNameToMenu, ConfigThingy menuConf, 
      String menuName, JComponent menu)
  {
    if (mapMenuNameToMenu != null && mapMenuNameToMenu.containsKey(menuName)) 
      return (JComponent)mapMenuNameToMenu.get(menuName);
    
    if (mapMenuNameToMenu == null && alreadySeen.contains(menuName))
    {
      Logger.error("Men� \""+menuName+"\" ist an einer Endlosschleife sich gegenseitig enthaltender Men�s beteiligt");
      return null;
    }

    ConfigThingy conf;
    try
    {
      conf = menuConf.query(menuName).getLastChild().get("Elemente");
    }
    catch (Exception x)
    {
      Logger.error("Men� \"" + menuName + "\" nicht definiert oder enth�lt keinen Abschnitt \"Elemente()\"");
      return null;
    }
    
    /*
     * Zur Vermeidung von Endlosschleifen m�ssen die folgenden BEIDEN Statements 
     * vor dem Aufruf von addUIElementsChecked stehen.
     */
    alreadySeen.add(menuName);
    if (mapMenuNameToMenu != null) mapMenuNameToMenu.put(menuName, menu);
    
    addUIElementsChecked(alreadySeen, menuConf, conf, menu, 0, 1, "menu");
    alreadySeen.remove(menuName);
    return menu;
  }
  
  /**
   * Initialisiert uiElementFactory.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void initFactories()
  {
    Map mapTypeToLayoutConstraints = new HashMap();
    Map mapTypeToLabelType = new HashMap();
    Map mapTypeToLabelLayoutConstraints = new HashMap();

    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    GridBagConstraints gbcCombobox  = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,           new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel =     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcButton    = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcHsep      = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
    GridBagConstraints gbcVsep      = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,       new Insets(0,TF_BORDER,0,TF_BORDER),0,0);
    GridBagConstraints gbcGlue      = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    
    
    mapTypeToLayoutConstraints.put("default", gbcButton);
    mapTypeToLabelType.put("default", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("default", null);
    
    mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
    mapTypeToLabelType.put("combobox", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("combobox", null);
    
    mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
    mapTypeToLabelType.put("h-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-glue", null);
    mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
    mapTypeToLabelType.put("v-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-glue", null);
    
    mapTypeToLayoutConstraints.put("label", gbcLabel);
    mapTypeToLabelType.put("label", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("label", null);
    
    mapTypeToLayoutConstraints.put("button", gbcButton);
    mapTypeToLabelType.put("button", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("button", null);
    
    mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
    mapTypeToLabelType.put("h-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-separator", null);
    mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
    mapTypeToLabelType.put("v-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-separator", null);

    UIElementEventHandler myUIElementEventHandler = new MyUIElementEventHandler();
    
    panelContext = new UIElementFactory.Context();
    panelContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    panelContext.mapTypeToLabelType = mapTypeToLabelType;
    panelContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    panelContext.uiElementEventHandler = myUIElementEventHandler;
    panelContext.mapTypeToType = new HashMap();
    panelContext.mapTypeToType.put("separator","v-separator");
    panelContext.mapTypeToType.put("glue","h-glue");
    
    menuContext = new UIElementFactory.Context();
    menuContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    menuContext.mapTypeToLabelType = mapTypeToLabelType;
    menuContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    menuContext.uiElementEventHandler = myUIElementEventHandler;
    menuContext.mapTypeToType = new HashMap();
    menuContext.mapTypeToType.put("separator","h-separator");
    menuContext.mapTypeToType.put("glue","v-glue");
    menuContext.mapTypeToType.put("button", "menuitem");
    
    Set supportedActions = new HashSet();
    supportedActions.add("openTemplate");
    supportedActions.add("absenderAuswaehlen");
    supportedActions.add("openDocument");
    supportedActions.add("dumpInfo");
    supportedActions.add("abort");
    supportedActions.add("kill");
    supportedActions.add("about");
    
    panelContext.supportedActions = supportedActions;
    menuContext.supportedActions = supportedActions;
    
    uiElementFactory = new UIElementFactory();
  }
  
  /**
   * Behandelt die Events der Eingabeelemente, die �ber die uiElementFactory 
   * erzeugt wurden (also fast alle).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyUIElementEventHandler implements UIElementEventHandler
  {
    public void processUiElementEvent(UIElement source, String eventType, Object[] args)
    {
      if (!eventType.equals("action")) return;
      
      String action = args[0].toString();
      if (action.equals("absenderAuswaehlen"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmAbsenderAuswaehlen,"");
      }
      else if (action.equals("openDocument"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmOpenDocument, args[1].toString());
      }
      else if (action.equals("openTemplate"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmOpenTemplate, args[1].toString());
      }
      else if (action.equals("dumpInfo"))
      {
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmDumpInfo, null);
      }
      else if (action.equals("abort"))
      {
        abort();
      }
      else if (action.equals("kill"))
      {
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmKill, null);
        abort();
      }
      else if (action.equals("about"))
      {
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmAbout, getBuildInfo());
      }
    }
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    eventHandler.handleTerminate();
    myFrame.dispose();
    eventHandler.waitForThreadTermination();

    System.exit(0);
  }  

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMuxBar zur�ck. Der Build-Status wird w�hrend dem
   * Build-Prozess mit dem Kommando "svn info" auf das Projektverzeichnis
   * erstellt. Die Buildinfo-Datei buildinfo enth�lt die Paketnummer und die
   * svn-Revision und ist in der Datei WollMuxBar.jar enthalten.
   * 
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende
   * Ersatzmeldung erzeugt (siehe Sourcecode).
   * 
   * @return Der Build-Status der aktuellen WollMuxBar.
   */
  public String getBuildInfo()
  {
    try
    {
      URL url = WollMuxBar.class.getClassLoader()
          .getResource("buildinfo");
      if (url != null)
      {
        BufferedReader in = new BufferedReader(new InputStreamReader(url
            .openStream()));
        return in.readLine().toString();
      }
    }
    catch (java.lang.Exception x)
    {
    }
    return "Version: unbekannt";
  }

  /**
   * Wird aufgerufen, wenn ein Button aktiviert wird, dem ein Men� zugeordnet
   * ist und l�sst dann das entsprechende Men� aus mapMenuNameToJPopupMenu 
   * erscheinen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void openMenu(ActionEvent e)
  {
    String menuName = e.getActionCommand();
    JComponent compo;
    try{
      compo = (JComponent)e.getSource();
    }catch(Exception x)
    {
      Logger.error(x);
      return;
    }
    
    JPopupMenu menu = (JPopupMenu)mapMenuNameToJPopupMenu.get(menuName);
    if (menu == null) return;
    
    menu.show(compo, 0, compo.getHeight());
  }

  /**
   * Diese Methode wird aufgerufen, wenn in der Senderbox ein anderes Element
   * ausgew�hlt wurde und setzt daraufhin den aktuellen Absender im
   * entfernten WollMux neu.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   * TESTED 
   */
  private void senderBoxItemChanged(ActionEvent e)
  {
    String[] str = e.getActionCommand().split(":",2);
    int index = Integer.parseInt(str[0]);
    String item = str[1];
    eventHandler.handleSelectPALEntry(item, index);
    minimize();
  }
  
  /**
   * Setzt die Eintr�ge aller Senderboxes neu.
   * @param entries die Eintr�ge, die die Senderboxen enthalten sollen.
   * @param current der ausgew�hlte Eintrag
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   * TESTED
   */
  public void updateSenderboxes(String[] entries, String current)
  {
    Iterator iter = senderboxes.iterator();
    while(iter.hasNext()) 
    {
      Senderbox senderbox = (Senderbox) iter.next();
      
      // alte Items l�schen
      senderbox.removeAllItems();
      
      // neue Items eintragen
      if(entries.length > 0) 
      {
        for (int i = 0; i < entries.length; i++)
        {
          senderbox.addItem(entries[i], senderboxActionListener, ""+i+":"+entries[i], myIsInsideMonitor);
        }
      } 
      else senderbox.addItem(LEERE_LISTE, null, null, myIsInsideMonitor);

      senderbox.addSeparator();
      senderbox.addItem("Liste Bearbeiten", actionListener_editSenderList, null, myIsInsideMonitor);
      
      if (current != null && !current.equals(""))
        senderbox.setSelectedItem(current);
    }
    
    setSizeAndLocation();
  }
  
  private static abstract class Senderbox
  {
    protected JComponent menu;
    
    public void removeAllItems()
    {
      menu.removeAll();
    }
    
    public void addItem(String item, ActionListener listen, String actionCommand, MouseListener mouseListen)
    {
      JMenuItem menuItem = new JMenuItem(item);
      menuItem.addActionListener(listen);
      menuItem.setActionCommand(actionCommand);
      menuItem.addMouseListener(mouseListen);
      menu.add(menuItem);
    }
    public void addSeparator()
    {
      menu.add(new JSeparator());
    }
    public abstract void setSelectedItem(String item);
    public static Senderbox create(JMenu menu)
    {
      return new JMenuSenderbox(menu);
    }
    public static Senderbox create(JPopupMenu menu, AbstractButton button)
    {
      return new JPopupMenuSenderbox(menu, button);
    }
    
    private static class JMenuSenderbox extends Senderbox
    {

      public JMenuSenderbox(JMenu menu)
      {
        this.menu = menu;
      }

      public void setSelectedItem(String item)
      {
        ((JMenu)menu).setText(item);
      }
    }
    
    private static class JPopupMenuSenderbox extends Senderbox
    {
      private AbstractButton button;

      public JPopupMenuSenderbox(JPopupMenu menu, AbstractButton button)
      {
        this.menu = menu;
        this.button = button;
      }

      public void setSelectedItem(String item)
      {
        button.setText(item);
      }
    }
  }
  
  /**
   * Erzeugt ein Popup-Fenster, das den Benutzer dar�ber informiert, dass keine
   * Verbindung zur WollMux-Komponente in OpenOffice hergestellt werden konnte.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void connectionFailedWarning()
  {
    JOptionPane.showMessageDialog(null, CONNECTION_FAILED_MESSAGE, "WollMux-Fehler", JOptionPane.ERROR_MESSAGE);
  }

  
  /**
   * Ein WindowListener, der auf die JFrames der Leiste 
   * registriert wird, damit als
   * Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgef�hrt wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener(){}
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { closeAction.actionPerformed(null); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) {}
  }
  
  /**
   * Wird auf das 
   * Leistenfenster als WindowFocusListener registriert, um falls erforderlich das
   * minimieren anzusto�en.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class WindowTransformer implements WindowFocusListener
  {
    public void windowGainedFocus(WindowEvent e) {}
    
    public void windowLostFocus(WindowEvent e)
    {
      minimize();
    }

  }
  
  /**
   * Wird auf den Strich am oberen Bildschirmrand registriert im UpAndAway Modus,
   * um darauf reagieren zu k�nnen, wenn die Maus dort eindringt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class UpAndAwayWindowTransformer implements MouseListener, ActionListener
  {
    private Timer timer;
    
    public UpAndAwayWindowTransformer() 
    {
      timer = new Timer(500, this);
      timer.setRepeats(false);
    }
    
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e)
    {
      timer.restart();
    }

    public void mouseExited(MouseEvent e) 
    {
      timer.stop();
    }

    public void actionPerformed(ActionEvent e)
    {
      maximize();
    }  
  }
  
  /**
   * Wird auf alle Komponenten der WollMuxBar registriert, um zu �berwachen,
   * ob die Maus in einer dieser Komponenten ist.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class IsInsideMonitor implements MouseListener, ActionListener
  {
    private Timer timer;

    public IsInsideMonitor()
    {
      timer = new Timer(1000, this);
      timer.setRepeats(false);
    }
    
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e)
    {
      if (windowMode != UP_AND_AWAY_WINDOW_MODE) return;
      timer.stop();
    }

    public void mouseExited(MouseEvent e)
    {
      if (windowMode != UP_AND_AWAY_WINDOW_MODE) return;
      timer.restart();
    }

    public void actionPerformed(ActionEvent e)
    {
      minimize();
    }
  }

  /**
   * Je nach windowMode wird die WollMuxBar auf andere Art und Weise in den
   * Wartezustand versetzt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void minimize()
  {
    if (windowMode == ALWAYS_ON_TOP_WINDOW_MODE || windowMode == NORMAL_WINDOW_MODE) return;
    if (windowMode == MINIMIZE_TO_TASKBAR_MODE) {myFrame.setExtendedState(Frame.ICONIFIED); return;}
    
    if (isMinimized) return;
    isMinimized = true;
    
    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.setJMenuBar(null);
      Container contentPane = myFrame.getContentPane();
      contentPane.remove(contentPanel);
      contentPane.add(upAndAwayMinimizedPanel);
      myFrame.setSize(minimizedWidth, 5);
      myFrame.addMouseListener(upAndAwayWindowTransformer);
    }
  }

  
  /**
   * Je nach windowMode wird die WollMuxBar aus dem Wartezustand wieder in den
   * aktiven Zustand versetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void maximize()
  {
    if (windowMode == MINIMIZE_TO_TASKBAR_MODE) {myFrame.setExtendedState(Frame.NORMAL); return;}
    
    if (!isMinimized) return;
    isMinimized = false;

    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.removeMouseListener(upAndAwayWindowTransformer);
      Container contentPane = myFrame.getContentPane();
      contentPane.remove(upAndAwayMinimizedPanel);
      contentPane.add(contentPanel);
      myFrame.setJMenuBar(menuBar);
      setSizeAndLocation();
    }
  }

  /**
   * �ffnet path als Vorlage.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void load(String path)
  {
    String urlStr = "";
    try{
      UNO.init();
      
      File curDir = new File(System.getProperty("user.dir"));
      File toOpen;
      if (path.charAt(0) != '/')
        toOpen = new File(curDir, path);
      else
        toOpen = new File(path);
      URL toOpenUrl = toOpen.toURI().toURL();
      urlStr = UNO.getParsedUNOUrl(toOpenUrl.toExternalForm()).Complete;
      UNO.loadComponentFromURL(urlStr, true, MacroExecMode.USE_CONFIG);
      System.exit(0);
    }catch(Exception x)
    {
      System.err.println("Versuch, URL \""+urlStr+"\" zu �ffnen gescheitert!");
      x.printStackTrace();
      System.exit(1);
    }
  }
  
  /**
   * Startet die WollMuxBar.
   * @param args --minimize, --topbar, --normalwindow um das Anzeigeverhalten festzulegen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args)
  {
    int windowMode = UP_AND_AWAY_WINDOW_MODE;
    if (args.length > 0)
    {
      if (args[0].equals("--minimize")) windowMode = MINIMIZE_TO_TASKBAR_MODE;
      else
      if (args[0].equals("--topbar")) windowMode = ALWAYS_ON_TOP_WINDOW_MODE;
      else
      if (args[0].equals("--normalwindow")) windowMode = NORMAL_WINDOW_MODE;
      else
      if (args[0].equals("--load"))
      {
        if (args.length < 2 || args[1].length() == 0) System.exit(0);
        load(args[1]);
      }
      else
      {
        System.err.println("Unbekannter Aufrufparameter: "+args[0]);
        System.exit(1);
      }
      
      if (args.length > 1)
      {
        System.err.println("Zu viele Aufrufparameter!");
        System.exit(1);
      }
    }
    
    WollMuxFiles.setupWollMuxDir();
    
    ConfigThingy wollmuxConf = WollMuxFiles.getWollmuxConf();
    
    try{
      Logger.debug("WollMuxBar gestartet");
      
      try{
        String windowMode2 = wollmuxConf.query("Fenster").query("WollMuxBar").getLastChild().query("MODE").getLastChild().toString();
        if (windowMode2.equalsIgnoreCase("AlwaysOnTop"))
          windowMode = ALWAYS_ON_TOP_WINDOW_MODE;
        else if (windowMode2.equalsIgnoreCase("Window"))
          windowMode = NORMAL_WINDOW_MODE;
        else if (windowMode2.equalsIgnoreCase("Minimize"))
          windowMode = MINIMIZE_TO_TASKBAR_MODE;
        else if (windowMode2.equalsIgnoreCase("UpAndAway"))
          windowMode = UP_AND_AWAY_WINDOW_MODE;
        else
          Logger.error("Ununterst�tzer MODE f�r WollMuxBar-Fenster: '"+windowMode2+"'");
      }catch(Exception x){}
      
      if (wollmuxConf.query("Symbolleisten").count()==0)
      {
        Logger.error(WOLLMUX_CONFIG_ERROR_MESSAGE);
        JOptionPane.showMessageDialog(null, WOLLMUX_CONFIG_ERROR_MESSAGE, "Fehlerhafte Konfiguration", JOptionPane.ERROR_MESSAGE);
      }
      else
        new WollMuxBar(windowMode, wollmuxConf);
      
    } catch(Exception x)
    {
      Logger.error(x);
    }
  }

}

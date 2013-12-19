package org.quasar.use2android.GUI;

import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import java.awt.Color;
import javax.swing.JButton;

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;


import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JTextField;
import javax.swing.DropMode;
import javax.swing.JTextPane;

import org.quasar.use2android.Prototype_CodeGeneration;

import java.awt.Toolkit;
import java.awt.Font;
import javax.swing.SwingConstants;

public class Gui {

	private JFrame frmJuseandroid;
	private final JTextField directory_input = new JTextField();
	private JTextField model_input;
	private JTextPane textPane;
	private JTextField ip1;
	private JTextField ip2;
	private JTextField ip4;
	private JTextField ip3;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			new StartScreen();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					
					Gui window = new Gui();
					window.frmJuseandroid.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Gui() {              
		initialize();
	}

	private static void inheritIO(final InputStream src, final PrintStream dest) {
	    new Thread(new Runnable() {
	        public void run() {
	            Scanner sc = new Scanner(src);
	            while (sc.hasNextLine()) {
	                dest.println(sc.nextLine());
	            }
	        }
	    }).start();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	String separator = System.getProperty("file.separator");
    String classpath = System.getProperty("java.class.path");
    String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
    ProcessBuilder processBuilder;
    Process p;
    MessageConsole console;
    private JTextField user;
    private JTextField pass;
    private JTextField port;
    private JTextField androidProjectName;
    private JTextField serverProjectName;
	
	private void initialize() {
		frmJuseandroid = new JFrame();
		frmJuseandroid.setResizable(false);
		frmJuseandroid.setIconImage(Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/guiImages/Android_Robot_100.png")));
		frmJuseandroid.setTitle("JUSE4Android");
		frmJuseandroid.setBounds(100, 100, 752, 634);
		frmJuseandroid.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmJuseandroid.getContentPane().setLayout(null);
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 746, 606);
		frmJuseandroid.getContentPane().add(panel);
		panel.setLayout(null);
		
//		processBuilder.redirectOutput(Redirect.INHERIT);
		
		final JButton btnGenerate = new JButton("Generate");
		btnGenerate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final Prototype_CodeGeneration generate = new Prototype_CodeGeneration();
				//validate input
				final String arg0_directory = directory_input.getText();
				final String arg1_model = model_input.getText();
				final String arg2_androidProjectName;
				if(!androidProjectName.getText().equals(""))
					arg2_androidProjectName = androidProjectName.getText();
				else
					arg2_androidProjectName = "empty";
				final String arg3_serverProjectName;
				if(!serverProjectName.getText().equals(""))
					arg3_serverProjectName = serverProjectName.getText();
				else
					arg3_serverProjectName = "empty";
				final String arg4_user;
				final String arg5_pass;
				final String arg6_port;
				if(!user.getText().equals(""))
					arg4_user = user.getText();
				else
					arg4_user = "empty";
				if(!pass.getText().equals(""))
					arg5_pass = pass.getText();
				else
					arg5_pass = "empty";
				if(!port.getText().equals(""))
					arg6_port = port.getText();
				else
					arg6_port = "empty";
				final String arg7_ip;
				if(ip1.getText().equals("") || ip1.getText().equals("") || ip2.getText().equals("") || ip3.getText().equals("") || ip4.getText().equals(""))
					arg7_ip = "empty";
				else
					arg7_ip = ip1.getText() + "." + ip2.getText() + "." + ip3.getText() + "." + ip4.getText();
//				System.out.println(arg0_directory + " - " + arg1_model + " - " + 
//						arg2_androidProjectName + " - " + arg3_serverProjectName + " - " + 
//						arg4_user + " - " + arg5_pass + " - " + 
//						arg6_port + " - " + arg7_ip);
				if(arg0_directory.equals(""))
					JOptionPane.showMessageDialog(null, "you must provide a target directory");
				else if(arg1_model.equals(""))
					JOptionPane.showMessageDialog(null, "you must provide a use (.use) model");
				else{
					try {
						processBuilder = new ProcessBuilder(path, "-cp", classpath, Prototype_CodeGeneration.class.getName(),
								arg0_directory, arg1_model, arg2_androidProjectName, arg3_serverProjectName,
								arg4_user, arg5_pass, arg6_port, arg7_ip);
						p = processBuilder.start();
	
						inheritIO(p.getInputStream(), System.out);
					    inheritIO(p.getErrorStream(), System.err);
						
						ProcessExitDetector processExitDetector = new ProcessExitDetector(p);
	
						processExitDetector.addProcessListener(new ProcessListener() {
						    public void processFinished(Process process) {
						    	System.out.println("\n -----------  DONE  ----------------\n\n");
						        btnGenerate.setEnabled(true);
						    }
						});
						processExitDetector.start();
						btnGenerate.setEnabled(false);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		btnGenerate.setBounds(5, 330, 89, 23);
		panel.add(btnGenerate);
		
		final JButton btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(".");
				
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				File workspace = new File(jfc.getCurrentDirectory() + "/");
				jfc.setCurrentDirectory(workspace);
				
				jfc.setDialogTitle("Open");
				jfc.setDialogType(JFileChooser.OPEN_DIALOG);

				int returnVal = jfc.showOpenDialog(btnBrowse);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					jfc.getSelectedFile();
				}
				


			}
		});
		btnBrowse.setBounds(647, 24, 89, 23);
		panel.add(btnBrowse);
		
		JButton button = new JButton("Browse");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(".");
				FileNameExtensionFilter filtro = new FileNameExtensionFilter("USE(*.use)", "use");
				jfc.setFileFilter(filtro);
				jfc.setAcceptAllFileFilterUsed(false);

				File workspace = new File(jfc.getCurrentDirectory() + "/");
				jfc.setCurrentDirectory(workspace);
				
				jfc.setDialogTitle("Open");
				jfc.setDialogType(JFileChooser.OPEN_DIALOG);

				int returnVal = jfc.showOpenDialog(btnBrowse);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					jfc.getSelectedFile();
				}
			}
		});
		button.setBounds(647, 72, 89, 23);
		panel.add(button);
		
		JLabel lblInputYourWorkspace = new JLabel("Input workspace - directory where project will be generated");
		lblInputYourWorkspace.setBounds(5, 11, 320, 14);
		panel.add(lblInputYourWorkspace);
		
		JLabel lblChooseTheModel = new JLabel("Choose the model file - (use extension)");
		lblChooseTheModel.setBounds(5, 58, 320, 14);
		panel.add(lblChooseTheModel);
		
		
		directory_input.setBounds(5, 25, 632, 20);
		panel.add(directory_input);
		directory_input.setColumns(10);
		directory_input.setDropTarget(new DropTarget() {
	        public synchronized void drop(DropTargetDropEvent evt) {
	            try {
	            	evt.acceptDrop(DnDConstants.ACTION_COPY);
	            	List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
	                for (File file : droppedFiles) {
	                	directory_input.setText(file.getAbsolutePath());
	                }
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
	    });
		
		model_input = new JTextField();
		model_input.setColumns(10);
		model_input.setBounds(5, 73, 632, 20);
		model_input.setDropTarget(new DropTarget() {
	        public synchronized void drop(DropTargetDropEvent evt) {
	            try {
	            	evt.acceptDrop(DnDConstants.ACTION_COPY);
	            	List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
	                for (File file : droppedFiles) {
	                	model_input.setText(file.getAbsolutePath());
	                }
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
	    });
		panel.add(model_input);
		
		JLabel lblServerIpAddress = new JLabel("Server ip address");
		lblServerIpAddress.setBounds(5, 258, 320, 14);
		panel.add(lblServerIpAddress);
		
		ip1 = new JTextField();
		ip1.setText("0");
		ip1.setColumns(10);
		ip1.setBounds(5, 272, 25, 20);
		panel.add(ip1);
		
		JLabel label = new JLabel(".");
		label.setBounds(33, 278, 9, 14);
		panel.add(label);
		
		ip2 = new JTextField();
		ip2.setText("0");
		ip2.setColumns(10);
		ip2.setBounds(40, 272, 25, 20);
		panel.add(ip2);
		
		ip4 = new JTextField();
		ip4.setText("0");
		ip4.setColumns(10);
		ip4.setBounds(110, 272, 25, 20);
		panel.add(ip4);
		
		JLabel label_1 = new JLabel(".");
		label_1.setBounds(103, 278, 9, 14);
		panel.add(label_1);
		
		ip3 = new JTextField();
		ip3.setText("0");
		ip3.setColumns(10);
		ip3.setBounds(75, 272, 25, 20);
		
		panel.add(ip3);
		
		JLabel label_2 = new JLabel(".");
		label_2.setBounds(66, 278, 9, 14);
		panel.add(label_2);
		
		JLabel lblNoteIfIp = new JLabel("note: if ip not set the default ip is set (Server - 127.0.0.1, Android - 10.0.2.2 )");
		lblNoteIfIp.setBounds(5, 295, 391, 14);
		panel.add(lblNoteIfIp);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(5, 364, 731, 231);
		panel.add(scrollPane);
		
		textPane = new JTextPane();
		scrollPane.setViewportView(textPane);
		textPane.setDropMode(DropMode.INSERT);
		textPane.setEditable(false);
		
		console = new MessageConsole(textPane);
		
		JLabel lblServer = new JLabel("Server");
		lblServer.setFont(new Font("Tahoma", Font.BOLD, 14));
		lblServer.setBounds(5, 143, 320, 14);
		panel.add(lblServer);
		
		JLabel lblUser = new JLabel("USER");
		lblUser.setBounds(5, 168, 320, 14);
		panel.add(lblUser);
		
		JLabel lblPassword = new JLabel("PASSWORD");
		lblPassword.setBounds(186, 168, 320, 14);
		panel.add(lblPassword);
		
		JLabel lblPort = new JLabel("PORT");
		lblPort.setBounds(5, 213, 320, 14);
		panel.add(lblPort);
		
		user = new JTextField();
		user.setColumns(10);
		user.setBounds(5, 182, 130, 20);
		panel.add(user);
		
		pass = new JTextField();
		pass.setColumns(10);
		pass.setBounds(186, 181, 130, 20);
		panel.add(pass);
		
		port = new JTextField();
		port.setText("0000");
		port.setColumns(10);
		port.setBounds(5, 227, 37, 20);
		panel.add(port);
		
		JLabel lblAndroidProjectName = new JLabel("Android project name");
		lblAndroidProjectName.setBounds(5, 104, 130, 14);
		panel.add(lblAndroidProjectName);
		
		JLabel lblServerProjectName = new JLabel("Server project name");
		lblServerProjectName.setBounds(186, 104, 130, 14);
		panel.add(lblServerProjectName);
		
		androidProjectName = new JTextField();
		androidProjectName.setColumns(10);
		androidProjectName.setBounds(5, 120, 130, 20);
		panel.add(androidProjectName);
		
		serverProjectName = new JTextField();
		serverProjectName.setColumns(10);
		serverProjectName.setBounds(186, 120, 130, 20);
		panel.add(serverProjectName);
		
		JLabel lblUseSameName = new JLabel("Use same name as the existing projects previously created\r\n");
		lblUseSameName.setVerticalAlignment(SwingConstants.TOP);
		lblUseSameName.setBounds(346, 104, 330, 14);
		panel.add(lblUseSameName);
		
		JLabel lblIfNotSupplied = new JLabel("If the names are not supplied the model name is used instead");
		lblIfNotSupplied.setVerticalAlignment(SwingConstants.TOP);
		lblIfNotSupplied.setBounds(346, 123, 330, 14);
		panel.add(lblIfNotSupplied);
		console.redirectOut();
	    console.redirectErr(Color.RED, null);
	}
}

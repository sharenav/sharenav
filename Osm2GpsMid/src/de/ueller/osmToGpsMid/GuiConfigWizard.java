/**
 * OSM2GpsMid 
 *  
 *
 * Copyright (C) 2008 Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;


public class GuiConfigWizard extends JFrame implements Runnable, ActionListener {

	Configuration config;
	String planet;
	JComboBox jcbProperties;

	boolean dialogFinished = false;

	public GuiConfigWizard(Configuration c) {
		this.config = c;
	}

	public void startWizard() {
		System.out.println("Starting configuration wizard");
		askOsmFile();
		setupWizard();
	}

	public void setupWizard() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		this.setLayout(gbl);

		JLabel jlPlanet = new JLabel("OsmFile: ");
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(jlPlanet, gbc);

		JTextField jtf = new JTextField(planet);
		jtf.setEditable(false);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 1;
		gbc.gridy = 0;
		add(jtf, gbc);

		Vector<String> propertiesName = enumerateBuiltinProperties();
		propertiesName.add(0, "custom .properties file");
		propertiesName.add(0, "use entire .osm file");
		jcbProperties = new JComboBox(propertiesName.toArray());
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		add(jcbProperties, gbc);

		JButton jbOk = new JButton("Ok");
		jbOk.setActionCommand("OK-click");
		jbOk.addActionListener(this);
		gbc.gridwidth = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		add(jbOk, gbc);

		JButton jbCancel = new JButton("Cancel");
		jbCancel.setActionCommand("Cancel-click");
		jbCancel.addActionListener(this);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.weighty = 1;
		gbc.gridx = 1;
		gbc.gridy = 2;
		add(jbCancel, gbc);

		this.setSize(400, 200);
		// pack();
		setVisible(true);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Window closing");
				if (dialogFinished != true) {
					System.exit(0);
				}
			}
		});

		Thread t = new Thread(this);
		t.start();
		try {
			t.join();
			System.out.println("Thread finished");
		} catch (InterruptedException e) {
			// Nothing to do
		}
	}

	private Vector<String> enumerateBuiltinProperties() {
		Vector<String> res = new Vector<String>();
		try {
			File jarFileName = new File(this.getClass().getProtectionDomain()
					.getCodeSource().getLocation().toURI());
			JarFile jarFile = new JarFile(jarFileName);
			Enumeration<JarEntry> jes = jarFile.entries();
			while (jes.hasMoreElements()) {
				String entryName = jes.nextElement().getName();
				if (entryName.endsWith(".properties")
						&& !entryName.endsWith("version.properties")) {
					res.add(entryName.substring(0, entryName.length() - 11));
				}

			}
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	private void askOsmFile() {

		JFileChooser chooser = new JFileChooser();
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory() || f.getAbsolutePath().endsWith(".osm")
						|| f.getAbsolutePath().endsWith(".osm.bz2")
						|| f.getAbsolutePath().endsWith(".osm.gz"))
					return true;

				return false;
			}

			@Override
			public String getDescription() {
				return "Openstreetmap file";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			planet = chooser.getSelectedFile().getAbsolutePath();
		} else {
			planet = "osmXapi";
		}
		config.setPlanetName(planet);
	}

	private void askPropFile() {
		JFileChooser chooser = new JFileChooser();
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()
						|| f.getAbsolutePath().endsWith(".properties"))
					return true;

				return false;
			}

			@Override
			public String getDescription() {
				return ".properties files";
			}

		};
		chooser.setFileFilter(ff);
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String propName = chooser.getSelectedFile().getAbsolutePath();
			config.setPropFileName(propName
					.substring(0, propName.length() - 11));
		}

	}

	/*
	 * Simply do nothing while the the dialog is still open. This is used to
	 * block on the dialog and make it modal
	 */
	@Override
	public void run() {
		while (!dialogFinished) {

			synchronized (this) {
				try {
					this.wait(1000);
				} catch (InterruptedException e) {
					// Nothing to do
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("OK-click".equalsIgnoreCase(e.getActionCommand())) {
			String chosenProperty = (String) jcbProperties.getSelectedItem();
			if (chosenProperty.equalsIgnoreCase("use entire .osm file")) {

			} else if (chosenProperty
					.equalsIgnoreCase("custom .properties file")) {
				askPropFile();
			} else {
				config.setPropFileName(chosenProperty);
			}

			dialogFinished = true;
			setVisible(false);
			dispose();
		}
		if ("Cancel-click".equalsIgnoreCase(e.getActionCommand())) {
			System.exit(0);
		}

	}

}

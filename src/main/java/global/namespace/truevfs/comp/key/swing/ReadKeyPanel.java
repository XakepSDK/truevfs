/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing;

import global.namespace.truevfs.comp.key.api.prompting.PromptingPbeParameters;

import javax.annotation.Nullable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * This panel prompts the user for a key to open an existing protected
 * resource.
 * It currently supports password and key file authentication.
 * <p>
 * Note that the contents of the password and file path fields are stored in
 * a static field from which they are restored when a new panel is created.
 * This is very convenient for the user if she inadvertently entered a wrong
 * key or shares the same key for multiple protected resources.
 *
 * @author Christian Schlichtherle
 */
final class ReadKeyPanel extends KeyPanel {

    private static final long serialVersionUID = 0;

    private static final ResourceBundle
            resources = ResourceBundle.getBundle(ReadKeyPanel.class.getName());

    private final SwingPromptingPbeParametersView<?, ?> view;
    private final Color defaultForeground;

    /** Constructs a new read key panel. */
    ReadKeyPanel(final SwingPromptingPbeParametersView<?, ?> view) {
        assert null != view;
        this.view = view;
        initComponents();
        final DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setError(null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setError(null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setError(null);
            }
        };
        passwdField.getDocument().addDocumentListener(dl);
        authenticationPanel.getKeyFileDocument().addDocumentListener(dl);
        defaultForeground = resource.getForeground();
    }

    @Override
    public URI getResource() {
        return URI.create(resource.getText());
    }

    @Override
    public void setResource(final URI resource) {
        final @Nullable URI lastResource = view.getLastResource();
        if (null != lastResource && !resource.equals(lastResource)) {
            this.resource.setForeground(Color.RED);
        } else {
            this.resource.setForeground(defaultForeground);
        }
        this.resource.setText(resource.toString());
        view.setLastResource(resource);
    }

    @Override
    public String getError() {
        final String error = this.error.getText();
        return error.isEmpty() ? null : error;
    }

    @Override
    public void setError(final String error) {
        this.error.setText(error);
    }

    @Override
    void updateParamChecked(final PromptingPbeParameters<?, ?> param)
    throws AuthenticationException {
        switch (authenticationPanel.getAuthenticationMethod()) {
            case AuthenticationPanel.AUTH_PASSWD:
                final char[] passwd = passwdField.getPassword();
                try {
                    param.setPassword(passwd);
                } finally {
                    Arrays.fill(passwd, (char) 0);
                }
                break;
            case AuthenticationPanel.AUTH_KEY_FILE:
                final File keyFile = authenticationPanel.getKeyFile();
                SwingPromptingPbeParametersView
                        .setPasswordOn(param, keyFile, false);
                break;
            default:
                throw new AssertionError("Unsupported authentication method!");
        }
        param.setChangeRequested(isChangeKeySelected());
    }

    /**
     * Getter for property changeKeySelected.
     *
     * @return Value of property changeKeySelected.
     */
    public boolean isChangeKeySelected() {
        return changeKey.isSelected();
    }

    /**
     * Setter for property changeKeySelected.
     *
     * @param changeKeySelected New value of property changeKeySelected.
     */
    public void setChangeKeySelected(boolean changeKeySelected) {
        this.changeKey.setSelected(changeKeySelected);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        final global.namespace.truevfs.comp.key.swing.util.EnhancedPanel passwdPanel = new global.namespace.truevfs.comp.key.swing.util.EnhancedPanel();
        final javax.swing.JLabel passwdLabel = new javax.swing.JLabel();
        final javax.swing.JLabel prompt = new javax.swing.JLabel();
        resource = new javax.swing.JTextPane();

        passwdPanel.addPanelListener(new global.namespace.truevfs.comp.key.swing.util.PanelListener() {
            public void ancestorWindowShown(global.namespace.truevfs.comp.key.swing.util.PanelEvent evt) {
                passwdPanelAncestorWindowShown(evt);
            }
            public void ancestorWindowHidden(global.namespace.truevfs.comp.key.swing.util.PanelEvent evt) {
            }
        });
        passwdPanel.setLayout(new java.awt.GridBagLayout());

        passwdLabel.setDisplayedMnemonic(resources.getString("passwd").charAt(0));
        passwdLabel.setLabelFor(passwdField);
        passwdLabel.setText(resources.getString("passwd")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        passwdPanel.add(passwdLabel, gridBagConstraints);

        passwdField.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        passwdPanel.add(passwdField, gridBagConstraints);

        setLayout(new java.awt.GridBagLayout());

        prompt.setLabelFor(resource);
        prompt.setText(resources.getString("prompt")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(prompt, gridBagConstraints);

        resource.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        resource.setEditable(false);
        resource.setFont(resource.getFont().deriveFont(resource.getFont().getStyle() | java.awt.Font.BOLD));
        resource.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        add(resource, gridBagConstraints);

        authenticationPanel.setPasswdPanel(passwdPanel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(authenticationPanel, gridBagConstraints);

        changeKey.setText(resources.getString("changeKey")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(changeKey, gridBagConstraints);

        error.setForeground(java.awt.Color.red);
        error.setName("error"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        add(error, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void passwdPanelAncestorWindowShown(global.namespace.truevfs.comp.key.swing.util.PanelEvent evt) {//GEN-FIRST:event_passwdPanelAncestorWindowShown
        // These are the things I hate Swing for: All I want to do here is to
        // set the focus to the passwd field in this panel when it shows.
        // However, this can't be done in the constructor since the panel is
        // not yet placed in a window which is actually showing.
        // Right, then we use this event listener to do it. This listener
        // method is called when the ancestor window is showing (and coding
        // the event generation was a less than trivial task).
        // But wait, simply setting the focus in this event listener here is
        // not possible on Linux because the containing window (now we have
        // one) didn't gain the focus yet.
        // Strangely enough, this works on Windows.
        // Even more strange, not even calling passwd.requestFocus() makes it
        // work on Linux!
        // So we add a window focus listener here and remove it when we
        // receive a Focus Gained Event.
        // But wait, then we still can't request the focus: This time it
        // doesn't work on Windows, while it works on Linux.
        // I still don't know the reason why, but it seems we're moving too
        // fast, so I have to post a new event to the event queue which finally
        // sets the focus.
        // But wait, requesting the focus could still fail for some strange,
        // undocumented reason - I wouldn't be surprised anymore.
        // So we add a conditional to select the entire contents of the field
        // only if we can really transfer the focus to it.
        // Otherwise, users could get easily confused.
        // If you carefully read the documentation for requestFocusInWindow()
        // however, then you know that even if it returns true, there is still
        // no guarantee that the focus gets actually transferred...
        // This mess is insane (and I can hardly abstain from writing down
        // all the other insulting scatology which comes to my mind)!
        final Window window = evt.getSource().getAncestorWindow();
        window.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                window.removeWindowFocusListener(this);
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (passwdField.requestFocusInWindow())
                            passwdField.selectAll();
                    }
                });
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });
    }//GEN-LAST:event_passwdPanelAncestorWindowShown

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final global.namespace.truevfs.comp.key.swing.AuthenticationPanel authenticationPanel = new global.namespace.truevfs.comp.key.swing.AuthenticationPanel();
    private final javax.swing.JCheckBox changeKey = new javax.swing.JCheckBox();
    private final javax.swing.JLabel error = new javax.swing.JLabel();
    private final javax.swing.JPasswordField passwdField = new javax.swing.JPasswordField();
    javax.swing.JTextPane resource;
    // End of variables declaration//GEN-END:variables
}
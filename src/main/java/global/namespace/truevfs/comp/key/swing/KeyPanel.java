/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing;

import global.namespace.truevfs.comp.key.api.prompting.PromptingPbeParameters;
import global.namespace.truevfs.comp.key.swing.feedback.Feedback;
import global.namespace.truevfs.comp.key.swing.util.EnhancedPanel;
import global.namespace.truevfs.comp.key.swing.util.PanelEvent;
import global.namespace.truevfs.comp.key.swing.util.PanelListener;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Abstract panel for prompting for authentication keys.
 *
 * @author Christian Schlichtherle
 */
abstract class KeyPanel extends EnhancedPanel {

    private static final long serialVersionUID = 0;

    private @Nullable Feedback feedback;

    KeyPanel() { super.addPanelListener(new KeyPanelListener()); }

    /**
     * Returns the feedback to run when this panel is shown in its ancestor
     * window.
     */
    public @Nullable Feedback getFeedback() { return feedback; }

    /**
     * Sets the feedback to run when this panel is shown in its ancestor
     * window.
     */
    public void setFeedback(final @Nullable Feedback feedback) {
        this.feedback = feedback;
    }

    private void runFeedback() {
        final Feedback feedback = getFeedback();
        if (null != feedback) feedback.run(this);
    }

    /**
     * Getter for property {@code resource}.
     *
     * @return Value of property {@code resource}.
     */
    public abstract URI getResource();

    /**
     * Setter for property {@code resource}.
     *
     * @param resource New value of property {@code resource}.
     */
    public abstract void setResource(final URI resource);

    /**
     * Getter for property {@code error}.
     */
    public abstract @Nullable String getError();

    /**
     * Setter for property error.
     *
     * @param error New value of property error.
     */
    public abstract void setError(@Nullable String error);

    final boolean updateParam(final PromptingPbeParameters<?, ?> param) {
        try {
            updateParamChecked(param);
            return true;
        } catch (final AuthenticationException ex) {
            setError(ex.getLocalizedMessage());
            return false;
        }
    }

    abstract void updateParamChecked(PromptingPbeParameters<?, ?> param)
    throws AuthenticationException;

    private static class KeyPanelListener implements PanelListener {
        @Override
        public void ancestorWindowShown(final PanelEvent evt) {
            ((KeyPanel) evt.getSource()).runFeedback();
        }

        @Override public void ancestorWindowHidden(PanelEvent evt) { }
    }
}

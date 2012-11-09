/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.param;

import net.java.truevfs.key.spec.prompting.PromptingPbeParametersTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class AesPbeParametersTest
extends PromptingPbeParametersTestSuite<AesPbeParameters, AesKeyStrength> {

    @Override
    protected AesPbeParameters newParam() { return new AesPbeParameters(); }
}

//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for class {@link TextUtil}.
 *
 * @see TextUtil
 */
public class TextUtilTest
{

    @Test
    public void testMaxStringLengthReturningNonEmptyString()
    {
        assertEquals("o,A...q{g", TextUtil.maxStringLength(9, "o,AE`s6y-Wsq{g"));
    }

    @Test
    public void testMaxStringLengthReturningEmptyString()
    {
        assertEquals("", TextUtil.maxStringLength(0, "\"\""));
    }

    @Test
    public void testHintWithNull()
    {
        assertEquals("<null>", TextUtil.hint(null));
    }

    @Test
    public void testHintWithEmptyString()
    {
        assertEquals("\"\"", TextUtil.hint(""));
    }
}

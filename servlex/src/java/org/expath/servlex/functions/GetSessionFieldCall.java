/****************************************************************************/
/*  File:       GetSessionFieldCall.java                                    */
/*  Author:     F. Georges - H2O Consulting                                 */
/*  Date:       2010-06-10                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2010 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.servlex.functions;

import java.util.Map;
import javax.servlet.ServletException;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import org.apache.log4j.Logger;
import org.expath.servlex.Servlex;

/**
 * TODO: Doc...
 *
 * @author Florent Georges
 * @date   2010-06-10
 */
public class GetSessionFieldCall
        extends ExtensionFunctionCall
{
    @Override
    public SequenceIterator call(SequenceIterator[] params, XPathContext ctxt)
            throws XPathException
    {
        // num of params
        if ( params.length != 1 ) {
            throw new XPathException("There is not exactly 1 param: " + params.length);
        }
        // the first param
        Item first = params[0].next();
        if ( first == null ) {
            throw new XPathException("The 1st param is an empty sequence");
        }
        if ( params[0].next() != null ) {
            throw new XPathException("The 1st param sequence has more than one item");
        }
        if ( ! ( first instanceof StringValue ) ) {
            throw new XPathException("The 1st param is not a string");
        }
        String name = first.getStringValue();
        // getting the sequence in the session
        try {
            LOG.debug("Get session field: '" + name + "'");
            Map<String, SequenceIterator> session = Servlex.getSessionMap();
            SequenceIterator sequence = session.get(name);
            if ( sequence == null ) {
                return EmptyIterator.getInstance();
            }
            return sequence.getAnother();
        }
        catch ( ServletException ex ) {
            throw new XPathException("Error in the Servlex session management", ex);
        }
    }

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(GetSessionFieldCall.class);
}


/* ------------------------------------------------------------------------ */
/*  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS COMMENT.               */
/*                                                                          */
/*  The contents of this file are subject to the Mozilla Public License     */
/*  Version 1.0 (the "License"); you may not use this file except in        */
/*  compliance with the License. You may obtain a copy of the License at    */
/*  http://www.mozilla.org/MPL/.                                            */
/*                                                                          */
/*  Software distributed under the License is distributed on an "AS IS"     */
/*  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See    */
/*  the License for the specific language governing rights and limitations  */
/*  under the License.                                                      */
/*                                                                          */
/*  The Original Code is: all this file.                                    */
/*                                                                          */
/*  The Initial Developer of the Original Code is Florent Georges.          */
/*                                                                          */
/*  Contributor(s): none.                                                   */
/* ------------------------------------------------------------------------ */

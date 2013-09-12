/****************************************************************************/
/*  File:       ConfigParamCall.java                                        */
/*  Author:     F. Georges - H2O Consulting                                 */
/*  Date:       2013-08-22                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2013 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.servlex.processors.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import org.apache.log4j.Logger;
import org.expath.servlex.TechnicalException;
import org.expath.servlex.processors.saxon.SaxonHelper;

/**
 * Implements web:config-param().
 * 
 * Two different arities exist:
 *
 *     web:config-param($name as xs:string) as xs:string?
 * 
 *     web:config-param($name    as xs:string,
 *                      $default as xs:string?) as xs:string?
 *
 * @author Florent Georges
 * @date   2013-08-22
 */
public class ConfigParamCall
        extends ExtensionFunctionCall
{
    @Override
    public SequenceIterator call(SequenceIterator[] orig_params, XPathContext ctxt)
            throws XPathException
    {
        // the params
        FunParams params = new FunParams(orig_params, 1, 2);
        String name = params.asString(0, false);
        String dflt = null;
        if ( params.number() == 2 ) {
            dflt = params.asString(1, true);
        }
        // do it
        LOG.debug(params.format("web:config-param").param(name).param(dflt).value());
        String value = doit(name, dflt);
        try {
            return SaxonHelper.toSequenceIterator(value);
        }
        catch ( TechnicalException ex ) {
            throw new XPathException("Error in the data model", ex);
        }
    }

    private String doit(String name, String dflt)
            throws XPathException
    {
        throw new XPathException("Not implemented yet, getting config param: " + name);
    }

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(ConfigParamCall.class);
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
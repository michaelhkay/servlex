/****************************************************************************/
/*  File:       SaxonItem.java                                              */
/*  Author:     F. Georges - H2O Consulting                                 */
/*  Date:       2013-04-30                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2013 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.servlex.processors.saxon.model;

import net.sf.saxon.s9api.XdmItem;
import org.expath.servlex.processors.Item;
import org.expath.servlex.processors.Sequence;

/**
 * A document for Saxon.
 *
 * @author Florent Georges
 */
public class SaxonItem
        implements Item
{
    public SaxonItem(XdmItem item)
    {
        if ( item == null ) {
            throw new NullPointerException("Underlying item is null for Saxon item");
        }
        myItem = item;
    }

    public SaxonItem(net.sf.saxon.om.Item item)
    {
        if ( item == null ) {
            throw new NullPointerException("Underlying item is null for Saxon item");
        }
        myItem = AccessProtectedItem.wrap(item);
    }

    @Override
    public Sequence asSequence()
    {
        return new SaxonSequence(myItem);
    }

    @Override
    public String stringValue()
    {
        return myItem.getStringValue();
    }

    // TODO: Should be package visible, but is used in ParseBasicAuthCall
    // (which should use instead a method on SaxonHelper which should be moved
    // here...)
    public XdmItem getXdmItem()
    {
        return myItem;
    }

    static SaxonItem asSaxonItem(Item item)
    {
        if ( ! (item instanceof SaxonItem) ) {
            throw new IllegalStateException("Not a Saxon item: " + item);
        }
        return (SaxonItem) item;
    }

    // TODO: Should be package visible, but is used in SaxonHelper (which
    // should be moved here...)
    public static XdmItem getXdmItem(Item item)
    {
        SaxonItem sitem = asSaxonItem(item);
        return sitem.getXdmItem();
    }

    private XdmItem myItem;

    private static class AccessProtectedItem
            extends XdmItem
    {
        public static XdmItem wrap(net.sf.saxon.om.Item item)
        {
            return wrapItem(item);
        }
    }
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

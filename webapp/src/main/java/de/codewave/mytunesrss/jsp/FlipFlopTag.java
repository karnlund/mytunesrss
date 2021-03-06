/*
 * Copyright (c) 2007, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.jsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * de.codewave.mytunesrss.jsp.FlipFlopTag
 */
public class FlipFlopTag extends TagSupport {
    private static final Logger LOG = LoggerFactory.getLogger(FlipFlopTag.class);

    @Override
    public int doStartTag() throws JspException {
        return Tag.EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        String value1 = (String)pageContext.getAttribute("flipFlop_value1");
        String value2 = (String)pageContext.getAttribute("flipFlop_value2");
        String current = (String)pageContext.getAttribute("flipFlop_current");
        if (current.equals(value1)) {
            pageContext.setAttribute("flipFlop_current", value2);
        } else {
            pageContext.setAttribute("flipFlop_current", value1);
        }
        try {
            pageContext.getOut().print(pageContext.getAttribute("flipFlop_current"));
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not write to JSP writer.", e);
            }
        }
        return Tag.EVAL_PAGE;
    }
}
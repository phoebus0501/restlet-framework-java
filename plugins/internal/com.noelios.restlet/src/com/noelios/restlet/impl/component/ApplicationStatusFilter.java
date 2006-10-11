/*
 * Copyright 2005-2006 Noelios Consulting.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * http://www.opensource.org/licenses/cddl1.txt
 * If applicable, add the following below this CDDL
 * HEADER, with the fields enclosed by brackets "[]"
 * replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package com.noelios.restlet.impl.component;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.component.ApplicationDelegate;
import org.restlet.data.Representation;
import org.restlet.data.Status;

import com.noelios.restlet.impl.StatusFilter;

/**
 * Status filter that tries to obtain ouput representation from an application.
 * @author Jerome Louvel (contact@noelios.com) <a href="http://www.noelios.com/">Noelios Consulting</a>
 */
public class ApplicationStatusFilter extends StatusFilter
{
	/** The application delegate. */
	private ApplicationDelegate applicationDelegate;

	/**
	 * Constructor.
	 * @param applicationDelegate The application delegate.
	 */
	public ApplicationStatusFilter(ApplicationDelegate applicationDelegate)
	{
		super(applicationDelegate.getContext(), applicationDelegate.isStatusOverwrite(), applicationDelegate
				.getContactEmail(), "/");
	}

	/**
	 * Returns the application delegate.
	 * @return The application delegate.
	 */
	public ApplicationDelegate getApplicationDelegate()
	{
		return this.applicationDelegate;
	}

   /**
    * Returns an output representation for the given status.<br/> In order to customize the 
    * default representation, this method can be overriden. 
    * @param status The status to represent.
    * @param request The request handled.
    * @param response The response updated.
    * @return The representation of the given status.
    */
   public Representation getOutput(Status status, Request request, Response response)
   {
   	Representation result = getApplicationDelegate().getApplication().getOutput(status, request, response);
   	if(result == null) result = super.getOutput(status, request, response);
   	return result;
   }
   
}

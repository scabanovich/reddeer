/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.reddeer.gef.impl.editpart;

import org.jboss.reddeer.gef.matcher.IsEditPartOfInstance;

/**
 * EditPart implementation which is looking for a given instance of edit part.
 * 
 * @author Andrej Podhradsky (andrej.podhradsky@gmail.com)
 *
 */
public class DefaultEditPart extends AbstractEditPart {

	/**
	 * Finds an edit part with a given instance name.
	 *
	 * @param instance the instance
	 */
	public DefaultEditPart(String instance) {
		this(instance, 0);
	}

	/**
	 * Finds an edit part with a given instance name at the specified index.
	 * 
	 * @param instance
	 *            Instance name
	 * @param index
	 *            Index
	 */
	public DefaultEditPart(String instance, int index) {
		super(new IsEditPartOfInstance(instance), index);
	}

}

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
package org.jboss.reddeer.swt.test.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class ParameterizedHandler extends AbstractHandler {

	private static boolean toggledA = false;
	private static boolean toggledB = false;
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String parameter = event.getParameter("RedDeerParameterID");
		if (parameter.equals("A")){
			toggledA = true;
		}else if(parameter.equals("B")){
			toggledB = true;
		}
		return null;
	}
	
	public static boolean isToggledA() {
		return toggledA;
	}
	
	public static boolean isToggledB() {
		return toggledB;
	}
	

}

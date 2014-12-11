/*******************************************************************************
 * Copyright (c) 2011 Boris von Loesch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Boris von Loesch - initial API and implementation
 ******************************************************************************/
package de.vonloesch.pdf4eclipse.editors.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.vonloesch.pdf4eclipse.editors.PDFEditor;


/**
 * Handles different zoom request. 
 * 
 * @author Boris von Loesch
 *
 */
public class ScrollCurrentPageRightHandler extends AbstractHandler {

	@Override  
	public Object execute(ExecutionEvent event) throws ExecutionException {		
		if (!PDFEditor.ID.equals(HandlerUtil.getActiveEditorId(event))) return null;

		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor == null) return null;
		
		if (!(editor instanceof PDFEditor)) return null;
		
		PDFEditor pdfEditor = (PDFEditor) editor;
		pdfEditor.scrollCurrentPageRight();

		return null;
	}
}

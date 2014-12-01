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

import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import de.vonloesch.pdf4eclipse.editors.PDFEditor;
import de.vonloesch.pdf4eclipse.preferences.PreferenceConstants;


/**
 * Triggers a forward search in all open pdf editors. Opens the first
 * editor for which the search was successful.
 *
 * @author Boris von Loesch
 *
 */
public class ForwardSearchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IEditorPart editorPart = HandlerUtil.getActiveEditor(event);

		if (!(editorPart instanceof ITextEditor)) {
			return null;
		}

		IEclipsePreferences prefs = (new InstanceScope()).getNode(de.vonloesch.pdf4eclipse.Activator.PLUGIN_ID);
		if (!prefs.getBoolean(PreferenceConstants.AUTOBUILD_SEARCH_FORWARD, true)) {
			searchForward(editorPart);
			return null;
		}

		// auto-build project before searching forward when necessary
		if (editorPart != null) {
			// Save current editor before compiling
			editorPart.doSave(new NullProgressMonitor());

			IProject project = null;
			FileEditorInput editorInput = (FileEditorInput) editorPart.getEditorInput();
			if (editorInput != null && editorInput.getFile() != null) {
				project = editorInput.getFile().getProject();
			}
			final IProject fProject = project;

			if (project == null)
				return null;

			buildAndSearch(fProject, editorPart);


		}
		return null;
	}

	private void buildAndSearch(final IProject project, final IEditorPart editorPart) {
		Job buildJob = new Job("Build and search forward") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				} catch (CoreException _) {
					MessageDialog.openInformation(editorPart.getEditorSite().getShell(),
							"Forward search failed",
							"Cannot build project.");
				}
				return Status.OK_STATUS;
			}
		};

		buildJob.setPriority(Job.INTERACTIVE);
		buildJob.setUser(false);
		buildJob.schedule(0);
		buildJob.addJobChangeListener(new IJobChangeListener() {
			@Override
			public void sleeping(IJobChangeEvent event) {
			}

			@Override
			public void scheduled(IJobChangeEvent event) {
			}

			@Override
			public void running(IJobChangeEvent event) {
			}

			@Override
			public void done(IJobChangeEvent event) {
				// use asyncExec to avoid invalid thread access exception
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						searchForward(editorPart);
					}
				});
			}

			@Override
			public void awake(IJobChangeEvent event) {
			}

			@Override
			public void aboutToRun(IJobChangeEvent event) {
			}
		});
	}

	private void searchForward(IEditorPart editorPart) {
		//Get file and selection in current editor
		if (editorPart.getEditorInput() instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();
			String fileName = input.getFile().getRawLocation().toOSString();
			int lineNr = 0;

			ISelection selection = ((ITextEditor) editorPart).getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection)selection;
				lineNr = textSelection.getStartLine() + 1;
			}

			IWorkbenchPage page =  editorPart.getSite().getPage();
			IEditorReference[] ref = page.getEditorReferences();
			for (IEditorReference iEditorReference : ref) {
				if (PDFEditor.ID.equals(iEditorReference.getId())) {
					IEditorPart p = iEditorReference.getEditor(true);
					if (p == null) continue;
					if (p instanceof PDFEditor) {
						PDFEditor pdfeditor = (PDFEditor) p;
						int returnCode = pdfeditor.forwardSearch(fileName, lineNr);
						if (returnCode == PDFEditor.FORWARD_SEARCH_OK)
							return;
					}
				}
			}
			MessageDialog.openInformation(editorPart.getEditorSite().getShell(),
					"Forward search failed",
					"The position could not be found in any currently open pdf files.");
		}
	}
}
/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist.index;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.editor.js.JSPlugin;
import com.aptana.index.core.AbstractFileIndexingParticipant;
import com.aptana.index.core.Index;
import com.aptana.json.Schema;
import com.aptana.json.SchemaContext;
import com.aptana.json.SchemaHandler;

/**
 * @author klindsey
 */
public class JSCAFileIndexingParticipant extends AbstractFileIndexingParticipant
{

	/*
	 * (non-Javadoc)
	 * @see com.aptana.index.core.IFileStoreIndexingParticipant#index(java.util.Set, com.aptana.index.core.Index,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void index(Set<IFileStore> resources, Index index, IProgressMonitor monitor) throws CoreException
	{
		SubMonitor sub = SubMonitor.convert(monitor, resources.size() * 100);

		for (IFileStore file : resources)
		{
			if (sub.isCanceled())
			{
				throw new CoreException(Status.CANCEL_STATUS);
			}

			Thread.yield(); // be nice to other threads, let them get in before each file...

			this.indexFileStore(index, file, sub.newChild(100));
		}

		sub.done();
	}

	/**
	 * indexFileStore
	 * 
	 * @param index
	 * @param file
	 * @param monitor
	 */
	private void indexFileStore(Index index, IFileStore file, IProgressMonitor monitor)
	{
		SubMonitor sub = SubMonitor.convert(monitor, 100);

		if (file == null)
		{
			return;
		}

		try
		{
			InputStreamReader isr = null;

			sub.subTask(getIndexingMessage(index, file));

			try
			{
				JSCAReader reader = new JSCAReader();
				SchemaContext context = new SchemaContext();
				SchemaHandler handler = new SchemaHandler();

				context.setHandler(handler);

				InputStream stream = file.openInputStream(EFS.NONE, sub.newChild(20));
				isr = new InputStreamReader(stream);

				// parse
				reader.read(isr, context);
				
				Schema s = reader.getSchema();
			}
			catch (Throwable e)
			{
				JSPlugin.logError(e.getMessage(), e);
			}
			finally
			{
				if (isr != null)
				{
					try
					{
						isr.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		finally
		{
			sub.done();
		}
	}
}

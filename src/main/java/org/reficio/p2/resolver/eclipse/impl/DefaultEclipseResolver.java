/**
 * Copyright (c) 2012-2022 Reficio (TM), Jesse Gallagher All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.reficio.p2.resolver.eclipse.impl;

import org.reficio.p2.resolver.eclipse.EclipseResolver;
import org.reficio.p2.resolver.maven.impl.Aether;
import org.reficio.p2.resolver.maven.impl.facade.AetherFacade;

import com.ibm.commons.util.io.StreamUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.reficio.p2.logger.Logger;
import org.reficio.p2.resolver.eclipse.EclipseResolutionRequest;
import org.reficio.p2.resolver.eclipse.EclipseResolutionResponse;

public class DefaultEclipseResolver implements EclipseResolver {
	private final File target;
    private final List<?> repositories;
    
    public DefaultEclipseResolver(List<?> repositories, File target) {
		this.target = target;
		this.repositories = repositories;
	}

	@Override
	public EclipseResolutionResponse resolve(EclipseResolutionRequest request) {
		List<File> result = new ArrayList<>();
		result.add(resolveBundle(request));
        if (request.isSource()) {
        	result.add(resolveSource(request));
        }
        return new EclipseResolutionResponse(result);
	}
	
	private File resolveBundle(EclipseResolutionRequest request) {
        String name = request.getId() + "_" + request.getVersion() + ".jar";
        File result = download(name, target);
        if (result == null) {
            throw new RuntimeException(MessageFormat.format("Cannot resolve {0} from any given repository", name));
        }
        return result;
    }

    private File resolveSource(EclipseResolutionRequest request) {
        String name = request.getId() + ".source_" + request.getVersion() + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
        File result = download(name, target);
        if (result == null) {
            Logger.getLog().warn(MessageFormat.format("Cannot resolve {0} from any given repository", name));
        }
        return result;
    }

    private File download(String name, File destination) {
        File file = new File(destination, name);
        for (Object repository : repositories) {
        	AetherFacade aether = Aether.facade(repository);
            if ("p2".equals(aether.getRepositoryType(repository))) {
            	String url = aether.getRepositoryUrl(repository) + "/plugins/" + name;
                Logger.getLog().info("\tDownloading: " + url);
                try(
                	InputStream is = new URL(url).openStream();
                	OutputStream os = new FileOutputStream(file);
                ) {
                	StreamUtil.copyStream(is, os);
                	return file;
                } catch(Exception e) {
                	// Move on to the next repo
                	e.printStackTrace();
                }
            }
        }
    	return null;
    }

}

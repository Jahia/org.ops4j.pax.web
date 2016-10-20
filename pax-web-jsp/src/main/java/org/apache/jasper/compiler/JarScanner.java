/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.apache.jasper.compiler;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to traverse the class loader hierarchy and lookup for JAR files.
 * 
 * @see Tomcat's org.apache.tomcat.util.scan.StandardJarScanner
 * 
 * @author Sergiy Shyrkov
 */
final class JarScanner {

    private static String getJarFileName(URL url) {
        String path = url.getPath();
        int jarExtPos = path.lastIndexOf(".jar");
        if (jarExtPos == -1) {
            return null;
        }
        String name = null;
        int pathLength = path.length();
        boolean noTrailingSlash = jarExtPos == pathLength - ".jar".length();
        if (noTrailingSlash || jarExtPos == pathLength - ".jar".length() - 1 && path.charAt(pathLength - 1) == '/'
                || jarExtPos == pathLength - ".jar".length() - 2 && path.charAt(pathLength - 1) == '/'
                && path.charAt(pathLength - 2) == '!') {
            if (noTrailingSlash) {
                name = path.substring(path.lastIndexOf('/') + 1, jarExtPos);
            } else {
                name = path.substring(0, jarExtPos);
                name = name.substring(name.lastIndexOf('/') + 1);
            }
        }
        return name;
    }

    private static URL[] getURLs(ClassLoader loader) {
        URL[] foundUrls = null;
        if (loader instanceof URLClassLoader) {
            foundUrls = ((URLClassLoader) loader).getURLs();
        } else if (loader.getClass().getName().equals("org.jboss.modules.ModuleClassLoader")) {
            // special case with JBoss ModuleClassLoader
            try {
                Enumeration<URL> resources = loader.getResources("/");
                if (resources != null && resources.hasMoreElements()) {
                    List<URL> urls = new LinkedList<URL>();
                    while (resources.hasMoreElements()) {
                        URL u = resources.nextElement();
                        if (u.getPath().endsWith(".jar/")) {
                            urls.add(u);
                        }
                    }

                    foundUrls = urls.size() > 0 ? urls.toArray(new URL[] {}) : null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return foundUrls;
    }

    private static void process(URL url, JarScannerCallback callback) throws IOException {
        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            callback.scan((JarURLConnection) conn);
        } else {
            String urlStr = url.toString();
            String jarURL = null;
            if (urlStr.startsWith("file:") && urlStr.endsWith(".jar")) {
                jarURL = "jar:" + urlStr + "!/";
            } else if (urlStr.startsWith("vfs:") && urlStr.endsWith(".jar/")) {
                // we handle currently only exploded Jahia Web application deployment
                jarURL = "jar:file:" + urlStr.substring("vfs:".length(), urlStr.length() - 1) + "!/";
            }
            if (jarURL != null) {
                callback.scan((JarURLConnection) new URL(jarURL).openConnection());
            }
        }
    }

    public static void scan(ClassLoader loader, JarScannerCallback callback, HashSet<String> noTldJars) {
        SkipConfig skipCfg = noTldJars == null || noTldJars.isEmpty() ? SkipConfig.DEFAULT : new SkipConfig(noTldJars);

        // we won't scan the bootstrap CL
        ClassLoader bootstrapCL = ClassLoader.getSystemClassLoader().getParent();
        while (loader != null && loader != bootstrapCL) {
            URL[] urls = getURLs(loader);

            if (urls != null) {
                for (int i = 0; i < urls.length; i++) {
                    try {
                        URL url = urls[i];
                        String jarFileName = getJarFileName(url);
                        if (jarFileName == null || !skipCfg.matches(jarFileName)) {
                            process(url, callback);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            loader = loader.getParent();
        }
    }

    private JarScanner() {
        super();
    }
}

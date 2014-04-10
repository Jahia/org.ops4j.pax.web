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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Utility class that handles JAR names and JAR name patterns to be skipped from scanning.
 * 
 * @author Sergiy Shyrkov
 */
class SkipConfig {

    public static final SkipConfig DEFAULT;

    private static final Pattern DOT = Pattern.compile(".", Pattern.LITERAL);

    private static final Pattern QUESTION_MARK = Pattern.compile("?", Pattern.LITERAL);

    private static final Pattern STAR = Pattern.compile("*", Pattern.LITERAL);

    static {
        DEFAULT = new SkipConfig(getDeafaultJarsToSkipValue("tomcat.util.scan.DefaultJarScanner.jarsToSkip",
                "org.apache.catalina.startup.TldConfig.jarsToSkip", "org.jahia.TldConfig.jarsToSkip",
                "org.jahia.custom.TldConfig.jarsToSkip"));
    }

    private static String getDeafaultJarsToSkipValue(String... systemProperties) {
        String jarList = null;
        for (String name : systemProperties) {
            String value = System.getProperty(name, "");
            if (value != null && value.length() > 0) {
                jarList = jarList != null ? jarList + "," + value : value;
            }

        }
        return jarList;
    }

    private Set<Pattern> jarPatternsToSkip;

    private Set<String> jarsToSkip;

    /**
     * Initializes an instance of this class.
     * 
     * @param jarsToSkip
     *            set of of JAR files that should not be scanned using the JarScanner functionality
     */
    public SkipConfig(Set<String> jarsToSkip) {
        super();
        setJarsToSkip(jarsToSkip);
    }

    /**
     * Initializes an instance of this class.
     * 
     * @param jarsToSkip
     *            comma-separated list of JAR files that should not be scanned using the JarScanner functionality
     */
    public SkipConfig(String jarsToSkip) {
        super();
        if (jarsToSkip != null) {
            Set<String> names = new HashSet<String>();
            StringTokenizer tokenizer = new StringTokenizer(jarsToSkip, ", ");
            while (tokenizer.hasMoreElements()) {
                names.add(tokenizer.nextToken());
            }
            setJarsToSkip(names);
        }
    }

    /**
     * Checks if the specified JAR has to be skipped during TLD scan.
     * 
     * @param jarNameWithoutExtension
     *            the JAR file name without <code>.jar</code> extension
     * @return <code>true</code> if the specified JAR has to be skipped; <code>false</code> otherwise.
     */
    public boolean matches(String jarNameWithoutExtension) {
        boolean skip = false;
        if (jarsToSkip != null) {
            skip = jarsToSkip.contains(jarNameWithoutExtension);
        }
        if (!skip && jarPatternsToSkip != null) {
            for (Pattern p : jarPatternsToSkip) {
                if (p.matcher(jarNameWithoutExtension).matches()) {
                    skip = true;
                    break;
                }
            }
        }

        return skip;
    }

    private void setJarsToSkip(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }

        for (String token : names) {
            if (token.endsWith(".jar")) {
                token = token.substring(0, token.length() - ".jar".length());
            }
            if (token.indexOf('*') != -1 || token.indexOf('?') != -1) {
                // we have placeholders
                token = DOT.matcher(token).replaceAll("\\.");
                token = STAR.matcher(token).replaceAll(".*");
                token = QUESTION_MARK.matcher(token).replaceAll(".");

                if (jarPatternsToSkip == null) {
                    jarPatternsToSkip = new LinkedHashSet<Pattern>();
                }
                jarPatternsToSkip.add(Pattern.compile(token));
            } else {
                if (jarsToSkip == null) {
                    jarsToSkip = new HashSet<String>();
                }
                jarsToSkip.add(token);

            }
        }
    }
}

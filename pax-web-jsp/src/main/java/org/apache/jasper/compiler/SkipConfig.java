/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
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

/* $Id$
 *****************************************************************************
 * Copyright (c) 2009 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Neustupny
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2003-2008 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.language.java.reveng;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.argouml.model.Model;

/**
 * The parse state keep control of data during parsing.
 *
 * @author Marcus Andersson, Thomas Neustupny
 */
class ParseState {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ParseState.class);

    /**
     * When the classifier parse is finished, these features will be
     * removed from the model.
     */
    private Collection obsoleteFeatures;

    /**
     * When the classifier parse is finished, these inner classes
     * will be removed from the model.
     */
    private Collection obsoleteInnerClasses;

    /**
     * This prefix is appended to inner classes, if any.
     */
    private String classnamePrefix;

    /**
     * The available context for currentClassifier.
     */
    private Context context;

    /**
     * The classifier that is parsed for the moment.
     */
    private Object classifier;

    /**
     * Counter for anonymous innner classes.
     */
    private int anonymousClassCounter;

    /**
     * Represents the source file being parsed. In UML1, this is a component.
     * In UML2, this is an artifact.
     */
    private Object artifact;

    /**
     * Create a new parse state.
     *
     * @param model The model.
     * @param javaLangPackage The default package java.lang.
     */
    public ParseState(Object model, Object javaLangPackage) {
	obsoleteInnerClasses = new ArrayList();
	classifier = null;
	context =
	    new PackageContext(new PackageContext(null, model),
				   javaLangPackage);
	anonymousClassCounter = 0;
    }

    /**
     * Create a new parse state based on another parse state.
     *
     * @param previousState The base parse state.
     * @param mClassifier The new classifier being parsed.
     * @param currentPackage The current package being parsed.
     */
    public ParseState(ParseState previousState,
                      Object mClassifier,
                      Object currentPackage) {

        LOG.info("Parsing the state of " + mClassifier);

        classnamePrefix =
            previousState.classnamePrefix
            + Model.getFacade().getName(mClassifier)
            + "$";
        obsoleteFeatures =
            new ArrayList(Model.getFacade().getFeatures(mClassifier));
        obsoleteInnerClasses =
            new ArrayList(Model.getFacade().getOwnedElements(mClassifier));
        context =
            new OuterClassifierContext(
                    previousState.context,
                    mClassifier,
                    currentPackage,
                    classnamePrefix);
        classifier = mClassifier;
        anonymousClassCounter = previousState.anonymousClassCounter;
    }

    /**
     * Add a package to the current context.
     *
     * @param mPackage The package to add.
     */
    public void addPackageContext(Object mPackage) {
	context = new PackageContext(context, mPackage);
    }

    /**
     * Add a classifier to the current context.
     *
     * @param mClassifier The classifier to add.
     */
    public void addClassifierContext(Object mClassifier) {
	context = new ClassifierContext(context, mClassifier);
    }

    /**
     * @param c the source file being parsed
     * @deprecated since 0.30.2
     */
    public void addComponent(Object c) {
        setArtifact(c);
    }

    /**
     * @param c the source file being parsed
     */
    public void setArtifact(Object c) {
        this.artifact = c;
    }

    /**
     * @return the source file being parsed
     * @deprecated since 0.30.2
     */
    public Object getComponent() {
        return getArtifact();
    }

    /**
     * @return the source file being parsed
     */
    public Object getArtifact() {
        return artifact;
    }

    /**
     * Get the current context.
     *
     * @return The current context.
     */
    public Context getContext() {
	return context;
    }

    /**
     * Get the current classifier.
     *
     * @return The current classifier.
     */
    public Object getClassifier() {
	return classifier;
    }

    /**
     * Tell the parse state that an anonymous class is being parsed.
     *
     * @return The name of the anonymous class.
     */
    public String anonymousClass() {
	classnamePrefix =
	    classnamePrefix.substring(0, classnamePrefix.indexOf("$") + 1);
	anonymousClassCounter++;
	return (Integer.valueOf(anonymousClassCounter)).toString();
    }

    /**
     * Tell the parse state that an outer class is being parsed.
     */
    public void outerClassifier() {
	classnamePrefix = "";
	anonymousClassCounter = 0;
    }

    /**
     * Get the current classname prefix.
     *
     * @return The current classname prefix.
     */
    public String getClassnamePrefix() {
	return classnamePrefix;
    }

    /**
     * Tell the parse state that a classifier is an inner classifier
     * to the current parsed classifier.
     *
     * @param mClassifier The inner classifier.
     */
    public void innerClassifier(Object mClassifier) {
	obsoleteInnerClasses.remove(mClassifier);
    }

    /**
     * Remove features no longer in the source from the current
     * classifier in the model.
     */
    public void removeObsoleteFeatures() {
    	if (obsoleteFeatures == null) {
            return;
        }
        for (Object feature : obsoleteFeatures) {
            Model.getCoreHelper().removeFeature(classifier, feature);
            Model.getUmlFactory().delete(feature);
    	}
    }

    /**
     * Remove inner classes no longer in the source from the current
     * classifier in the model.
     */
    public void removeObsoleteInnerClasses() {
    	if (obsoleteInnerClasses == null) {
	    return;
	}
	for (Object element : obsoleteInnerClasses) {
	    if (Model.getFacade().isAClassifier(element)) {
		Model.getUmlFactory().delete(element);
	    }
	}
    }

    /**
     * Tell the parse state that a feature belongs to the current
     * classifier.
     *
     * @param feature The feature.
     */
    public void feature(Object feature) {
	obsoleteFeatures.remove(feature);
    }

    /**
     * Get a feature from the current classifier not yet modeled.
     *
     * @param name The name of the feature.
     * @return The found feature, null if not found.
     */
    public Object getFeature(String name) {
        for (Object mFeature : obsoleteFeatures) {
	    if (name.equals(Model.getFacade().getName(mFeature))) {
		return mFeature;
	    }
	}
	return null;
    }

    /**
     * Get a features from the current classifier not yet modeled.
     *
     * @param name The name of the feature.
     * @return The collection of found features
     */
    public Collection getFeatures(String name) {
    	ArrayList list = new ArrayList();
    	for (Object mFeature : obsoleteFeatures) {
	    if (name.equals(Model.getFacade().getName(mFeature))) {
		list.add(mFeature);
	    }
	}
	return list;
    }

    /**
     * Get a method from the current classifier not yet modeled.
     *
     * @param name The name of the method.
     * @return The found method, null if not found.
     */
    public Object getMethod(String name) {
        for (Object mFeature : obsoleteFeatures) {
	    if (Model.getFacade().isAMethod(mFeature)
		&& name.equals(Model.getFacade().getName(mFeature))) {
		return mFeature;
	    }
	}
	return null;
    }

    /**
     * Get a operation from the current classifier not yet modeled.
     *
     * @param name The name of the operation.
     * @return The found operation, null if not found.
     */
    public Object getOperation(String name) {
        for (Object feature : obsoleteFeatures) {
	    if (Model.getFacade().isAOperation(feature)
                    && name.equals(Model.getFacade().getName(feature))) {
		return feature;
	    }
	}
	return null;
    }

    /**
     * Get a attribute from the current classifier not yet modeled.
     *
     * @param name The name of the attribute.
     * @return The found attribute, null if not found.
     */
    public Object getAttribute(String name) {
        for (Object feature : obsoleteFeatures) {
            if (Model.getFacade().isAAttribute(feature)
                    && name.equals(Model.getFacade().getName(feature))) {
                return feature;
            }
        }
        return null;
    }
}

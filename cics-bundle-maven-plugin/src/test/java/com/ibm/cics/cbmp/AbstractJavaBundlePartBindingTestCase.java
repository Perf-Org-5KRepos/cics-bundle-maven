package com.ibm.cics.cbmp;

/*-
 * #%L
 * CICS Bundle Maven Plugin
 * %%
 * Copyright (C) 2019 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.artifact.Artifact;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.matchers.CompareMatcher;

import com.ibm.cics.bundle.parts.BundleResource;

public abstract class AbstractJavaBundlePartBindingTestCase {

	public static final String GROUP_ID = "com.example";
	public static final String ARTIFACT_ID = "the-artifact-id";
	public static final String VERSION = "1.0.0-SNAPSHOT";

	private AbstractAutoConfigureBundlePublisherMojo mojo;

	private Map<String, String> defineAttributes = new HashMap<>();
	
	protected Artifact artifact;
	protected AbstractJavaBundlePartBinding binding;
	
	protected abstract AbstractJavaBundlePartBinding createBinding();
	protected abstract String getRootElementName();
	
	@Before
	public void setUp() {
		artifact = mock(Artifact.class);
		mojo = mock(AbstractAutoConfigureBundlePublisherMojo.class);
		
		binding = createBinding();
		binding.setResolvedArtifact(artifact);
		
		when(artifact.getArtifactId()).thenReturn(ARTIFACT_ID);
		when(artifact.getBaseVersion()).thenReturn(VERSION);
		when(artifact.getGroupId()).thenReturn(GROUP_ID);
	}
	
	@Before
	public void defaultName() {
		setExpectedSymbolicName(artifact.getArtifactId() + "-" + artifact.getBaseVersion());
	}
	
	@Before
	public void defaultJvmServer() {
		when(mojo.getJVMServer()).thenReturn("MYJVMS");
		setExpectedJVMServer("MYJVMS");
	}
	
	protected void setExpectedJVMServer(String jvmServer) {
		this.defineAttributes.put("jvmserver", jvmServer);
	}
	
	protected void setExpectedSymbolicName(String symbolicname) {
		this.defineAttributes.put("symbolicname", symbolicname);
	}
	
	protected void setOtherExpectedAttributes(Map<String, String> otherAttributes) {
		this.defineAttributes.putAll(otherAttributes);
	}
	
	protected void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}
	
	protected void assertBundleResources() throws Exception {
		//assert bundle part
		BundleResource br = binding.toBundlePart(mojo);
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document actualBundlePart = documentBuilder.parse(br.getContent());
		
		Document expectedBundlePart = documentBuilder.newDocument();
		Element element = expectedBundlePart.createElement(getRootElementName());
		expectedBundlePart.appendChild(element);
		this.defineAttributes.forEach(element::setAttribute);
		
		assertThat(
			actualBundlePart,
			CompareMatcher
				.isIdenticalTo(
					expectedBundlePart
				).withDifferenceEvaluator(
					DifferenceEvaluators.chain(
						DifferenceEvaluators.ignorePrologDifferencesExceptDoctype(),
						DifferenceEvaluators.Default
					)
				)
			);
		
		//assert dynamic resources
		br.getDynamicResources();
	}
	
	@Test
	public void defaults() throws Exception {
		assertBundleResources();
	}
	
	@Test
	public void nameOverride() throws Exception {
		binding.setName("bananas");
		setExpectedSymbolicName("bananas");
		
		assertBundleResources();
	}
	
	@Test
	public void jvmServerOverride() throws Exception {
		binding.setJvmserver("OJVMS");
		setExpectedJVMServer("OJVMS");
		
		assertBundleResources();
	}
}

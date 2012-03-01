package org.grails.plugins.marshallers
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import grails.converters.XML

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;

import org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller;
import org.grails.plugins.marshallers.config.MarshallingConfig;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

class GenericDomainClassXMLMarshaller implements ObjectMarshaller<XML> {
	private static Log LOG = LogFactory.getLog(GenericDomainClassXMLMarshaller.class);
	private String configName;
	private final boolean includeVersion=true;
	private ProxyHandler proxyHandler;

	public GenericDomainClassXMLMarshaller(String configName, ProxyHandler proxyHandler){
		LOG.debug("Registered xml domain class marshaller for $configName");
		this.configName=configName;
		this.proxyHandler=proxyHandler;
	}

	@Override
	public boolean supports(Object object) {
		def clazz=object.getClass();
		return ConverterUtil.isDomainClass(clazz) && GCU.getStaticPropertyValue(clazz,'marshalling');
	}

	@Override
	public void marshalObject(Object value, XML xml)	throws ConverterException {
		Class clazz = value.getClass();
		GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
		def mc=MarshallingConfig.getForClass(clazz).getConfig('xml',configName);
		BeanWrapper beanWrapper = new BeanWrapperImpl(value);

		GrailsDomainClassProperty id = domainClass.getIdentifier();
		Object idValue = beanWrapper.getPropertyValue(id.getName());

		if (idValue != null) xml.attribute("id", String.valueOf(idValue));

		if (includeVersion) {
			Object versionValue = beanWrapper.getPropertyValue(domainClass.getVersion().getName());
			xml.attribute("version", String.valueOf(versionValue));
		}

		GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

		for (GrailsDomainClassProperty property : properties) {
			xml.startNode(property.getName());
			if (!property.isAssociation()) {
				// Write non-relation property
				Object val = beanWrapper.getPropertyValue(property.getName());
				xml.convertAnother(val);
			}
			else {
				Object referenceObject = beanWrapper.getPropertyValue(property.getName());
				if (isRenderDomainClassRelations()) {
					if (referenceObject != null) {
						referenceObject = proxyHandler.unwrapIfProxy(referenceObject);
						if (referenceObject instanceof SortedMap) {
							referenceObject = new TreeMap((SortedMap) referenceObject);
						}
						else if (referenceObject instanceof SortedSet) {
							referenceObject = new TreeSet((SortedSet) referenceObject);
						}
						else if (referenceObject instanceof Set) {
							referenceObject = new HashSet((Set) referenceObject);
						}
						else if (referenceObject instanceof Map) {
							referenceObject = new HashMap((Map) referenceObject);
						}
						else if (referenceObject instanceof Collection) {
							referenceObject = new ArrayList((Collection) referenceObject);
						}
						xml.convertAnother(referenceObject);
					}
				}
				else {
					if (referenceObject != null) {
						GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();

						// Embedded are now always fully rendered
						if (referencedDomainClass == null || property.isEmbedded() || GCU.isJdk5Enum(property.getType())) {
							xml.convertAnother(referenceObject);
						}
						else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
							asShortObject(referenceObject, xml, referencedDomainClass.getIdentifier(), referencedDomainClass);
						}
						else {
							GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
							@SuppressWarnings("unused")
									String refPropertyName = referencedDomainClass.getPropertyName();
							if (referenceObject instanceof Collection) {
								Collection o = (Collection) referenceObject;
								for (Object el : o) {
									xml.startNode(xml.getElementName(el));
									asShortObject(el, xml, referencedIdProperty, referencedDomainClass);
									xml.end();
								}
							}
							else if (referenceObject instanceof Map) {
								Map<Object, Object> map = (Map<Object, Object>) referenceObject;
								for (Map.Entry<Object, Object> entry : map.entrySet()) {
									String key = String.valueOf(entry.getKey());
									Object o = entry.getValue();
									xml.startNode("entry").attribute("key", key);
									asShortObject(o, xml, referencedIdProperty, referencedDomainClass);
									xml.end();
								}
							}
						}
					}
				}
			}
			xml.end();
		}

	}
	protected void asShortObject(Object refObj, XML xml, GrailsDomainClassProperty idProperty,
	@SuppressWarnings("unused") GrailsDomainClass referencedDomainClass) throws ConverterException {
		Object idValue;
		if(proxyHandler instanceof EntityProxyHandler) {

			idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
			if(idValue == null) {
				idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName());
			}

		}
		else {
			idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName());
		}
		xml.attribute("id",String.valueOf(idValue));
	}

	protected boolean isRenderDomainClassRelations() {
		return false;
	}
}

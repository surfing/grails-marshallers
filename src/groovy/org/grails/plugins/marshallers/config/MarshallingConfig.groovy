package org.grails.plugins.marshallers.config
/**
 * Marshalling configuration options
 * 
 * @author dhalupa
 *
 */
class MarshallingConfig {
	/**
	 * Inherited configuration name or null if root configuration
	 */
	MarshallingConfig _parent
	/**
	 * List of field names. If a field representing one-to-many relation is marked as deep, all contained data of related objects will be serialized
	 */
	List deep
	/**
	 *  list of fields which uniquely identifies a domain object in case database id is not sufficient.
	 */
	List identifier
	/**
	 * configures a custom domain object element name which should be used instead of default one
	 */
	String elementName
	/**
	 * list of field names which will be serialized as attributes of domain object element 
	 */
	List attribute
	/**
	 * list of field names which will be ignored during serialization
	 */
	List ignore
	/**
	 * configuration option allows us to define closures with custom serialization behavior
	 */
	Map virtual
	/**
	 * configuration option allows us to define closures with custom serialization behavior
	 */
	Map serializer
	/**
	 * when true will suppress serialization of domain object identifier
	 */
	boolean shouldOutputIdentifier=true
	/**
	 * json,xml or null
	 */
	String type
	/**
	 * name of named configuration or 'default'
	 */
	String name='default'
	/**
	 * If true domain instance class name will be serialized
	 */
	Boolean shouldOutputClass
	/**
	 * If true version information will be serialized
	 */
	Boolean shouldOutputVersion

	List children=[]

	def findConfigNames(type){
		def configs=[] as Set, worker
		worker={
			->
			if(delegate.type==null || delegate.type==type){
				configs<<delegate.name
			}
			delegate.children.each{child->
				worker.delegate=child
				worker()
			}
		}
		worker()
		configs
	}

	def findNamedConfig(type,name){
		def worker,result
		worker={->
			if((delegate.type==type || delegate.type==null) && name==delegate.name){
				result=delegate
			}
			delegate.children.each{child->
				worker.delegate=child
				worker()
			}
		}
		worker()
		result
	}
	
	def resolvePath(){
		def m=[] as List
		def c=this
		while(c._parent){
			m<<c.name
			c=c._parent
		}
		m.reverse()
	}
	
}

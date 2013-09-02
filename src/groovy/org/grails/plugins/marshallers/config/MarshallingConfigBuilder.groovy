package org.grails.plugins.marshallers.config
import java.util.List;
import java.util.Map;
/**
 * @author dhalupa
 */
class MarshallingConfigBuilder {
	MarshallingConfig config=new MarshallingConfig()
	
	
	MarshallingConfigBuilder(){
		
	}
	
	MarshallingConfigBuilder(MarshallingConfigBuilder parentBuilder,String name){
		config._parent=parentBuilder.config
		config.name=name
		config.type=parentBuilder.config.type
		
		['deep','identifier','elementName','attribute','virtual','shouldOutputIdentifier','shouldOutputClass','shouldOutputVersion','ignore'].each{p->
			if(config._parent[p]!=null){
				config[p]=config._parent[p]
			}
		}
	}


	void deep(String... args){
		config.deep=args as List
	}

	void identifier(String... args){
		config.identifier=args as List
	}
	
	void identifier(Closure arg){
		config.identifier=[arg]
	}

	void elementName(String arg){
		config.elementName=arg
	}
	void attribute(String... args){
		config.attribute=args as List
	}
	void ignore(String... args){
		config.ignore=args as List
	}
	void virtual(Closure arg){
		VirtualPropertiesBuilder builder=new VirtualPropertiesBuilder()
		arg.delegate=builder
		arg.resolveStrategy = Closure.DELEGATE_FIRST
		arg()
		config.virtual=builder.properties
	}
	
	void serializer(Closure arg){
		VirtualPropertiesBuilder builder=new VirtualPropertiesBuilder()
		arg.delegate=builder
		arg.resolveStrategy = Closure.DELEGATE_FIRST
		arg()
		config.serializer=builder.properties
	}
	/**
	 * use shouldOutputIdentifier instead
	 * @param arg
	 */
	@Deprecated
	void ignoreIdentifier(boolean arg){
		config.shouldOutputIdentifier = !arg
	}

	void shouldOutputIdentifier(boolean arg){
		config.shouldOutputIdentifier = arg
	}
	
	void shouldOutputClass(boolean arg){
		config.shouldOutputClass=arg
	}

	void shouldOutputVersion(boolean arg){
		config.shouldOutputVersion=arg
	}
	void xml(Closure arg){
		MarshallingConfigBuilder builder=new MarshallingConfigBuilder(config)
		builder.config.type='xml'
		arg.delegate=builder
		arg.resolveStrategy = Closure.DELEGATE_FIRST
		arg()
		config.children<<builder.config
	}
	void json(Closure arg){
		MarshallingConfigBuilder builder=new MarshallingConfigBuilder(config)
		builder.config.type='json'
		arg.delegate=builder
		arg.resolveStrategy = Closure.DELEGATE_FIRST
		arg()
		
		config.children<<builder.config
	}

	def methodMissing(String name,args){
		if(args.size()==1 && args[0] instanceof Closure){
			Closure c=args[0] as Closure
			MarshallingConfigBuilder builder=new MarshallingConfigBuilder(this,name)
			c.delegate=builder
			c.resolveStrategy = Closure.DELEGATE_FIRST
			config.children<<builder.config
			c()
		}else{
			throw new RuntimeException("Named configuration $name has to have single argument of Closure type")
		}
	}
	
	 
}

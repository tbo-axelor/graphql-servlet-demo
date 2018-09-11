package com.axelor.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.servlet.annotation.WebServlet;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import graphql.servlet.SimpleGraphQLServlet;

@WebServlet(urlPatterns = "/graphql")
public class GraphQLEndpoint extends SimpleGraphQLServlet {

	public GraphQLEndpoint() {
		super(buildSchema());
	}

	private static GraphQLSchema buildSchema() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("graphql-java");
		EntityManager em = emf.createEntityManager();
		return GraphQLSchema.newSchema().query(getQueryObject(em)).build();

	}
	
	private static GraphQLObjectType getQueryObject(EntityManager em) {
		return GraphQLObjectType.newObject().name("query").fields(em.getMetamodel()
				.getEntities().stream().map(t -> getFieldDefination(t, em)).collect(Collectors.toList())).build();
	}
	
	private static GraphQLOutputType findBasicType(Attribute<?, ?> attr) {
		Class<?> type = attr.getJavaType();
		if (String.class.isAssignableFrom(type)) {
			return Scalars.GraphQLString;
		} else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
			return Scalars.GraphQLInt;
		} else if (Short.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)) {
			return Scalars.GraphQLShort;
		} else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)
				|| Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
			return Scalars.GraphQLFloat;
		} else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
			return Scalars.GraphQLLong;
		} else if (Long.class.isAssignableFrom(type)) {
			return Scalars.GraphQLLong;
		} else if (BigDecimal.class.isAssignableFrom(type)) {
			return Scalars.GraphQLBigDecimal;
		} else {
			throw new UnsupportedOperationException(
	                "Class could not be mapped to GraphQL: '" + type.getClass().getTypeName() + "'");
		}
	}

	private static GraphQLOutputType findType(Attribute<?, ?> attr) {
		if(attr.getPersistentAttributeType() == PersistentAttributeType.BASIC) {
			return findBasicType(attr);
		} else if(attr.getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY || attr.getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY) {
			Type<?> foreignType = ((PluralAttribute<?, ?, ?>) attr).getElementType();
			return new GraphQLList(new GraphQLTypeReference(foreignType.getJavaType().getSimpleName()));
		} else if(attr.getPersistentAttributeType() == PersistentAttributeType.MANY_TO_ONE || attr.getPersistentAttributeType() == PersistentAttributeType.ONE_TO_ONE) {
			Type<?> foreignType = ((SingularAttribute<?, ?>) attr).getType();
			return new GraphQLTypeReference(foreignType.getJavaType().getSimpleName());
		} else {
			throw new UnsupportedOperationException(
	                "Attribute could not be mapped to GraphQL: field '" + attr.getDeclaringType().getJavaType().getName() + "' of entity class '" + attr.getJavaMember().getName() + "'");
		}
	}

	private static GraphQLFieldDefinition getFieldDefination(EntityType<?> entity, EntityManager entityManager) {
		return GraphQLFieldDefinition.newFieldDefinition().name(entity.getName()).type(new GraphQLList(getType(entity))).argument(getArgumentList())
				.dataFetcher(new JpaDataFetcher(entityManager, entity)).build();
	}

	private static GraphQLFieldDefinition getTypeField(Attribute<?, ?> attr) {
		return GraphQLFieldDefinition.newFieldDefinition().name(attr.getName()).type(findType(attr)).build();
	}

	private static GraphQLObjectType getType(EntityType<?> entity) {
		GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(entity.getName());
		for (Attribute<?, ?> attr : entity.getAttributes()) {
			builder.field(getTypeField(attr));
		}
		return builder.build();
	}

	private static GraphQLArgument getIdArgument() {
		return GraphQLArgument.newArgument().name("id").type(Scalars.GraphQLLong).build();
	}

	private static GraphQLArgument getFilterArgument() {
		return GraphQLArgument.newArgument().name("filter").type(Scalars.GraphQLString).build();
	}
	private static List<GraphQLArgument> getArgumentList() {
		List<GraphQLArgument> list = new ArrayList<>();
		list.add(getIdArgument());
		list.add(getFilterArgument());
		return list;
	}
}

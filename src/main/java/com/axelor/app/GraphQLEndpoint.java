package com.axelor.app;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.servlet.annotation.WebServlet;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.servlet.SimpleGraphQLServlet;

@WebServlet(urlPatterns = "/graphql")
public class GraphQLEndpoint extends SimpleGraphQLServlet {

	public GraphQLEndpoint() {
		super(buildSchema());
	}

	private static GraphQLSchema buildSchema() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("graphql-java");
		EntityManager em = emf.createEntityManager();
		return GraphQLSchema.newSchema().query(GraphQLObjectType.newObject().name("query").fields(em.getMetamodel()
				.getEntities().stream().map(t -> getFieldDefination(t, em)).collect(Collectors.toList()))).build();

	}

	private static GraphQLOutputType findType(Attribute<?, ?> attr) {
		Class<?> type = attr.getJavaType();
		if (String.class.isAssignableFrom(type)) {
			return Scalars.GraphQLString;
		} else if (Long.class.isAssignableFrom(type)) {
			return Scalars.GraphQLLong;
		}
		return Scalars.GraphQLString;
	}

	private static GraphQLFieldDefinition getFieldDefination(EntityType<?> entity, EntityManager entityManager) {
		return GraphQLFieldDefinition.newFieldDefinition().name(entity.getName()).type(new GraphQLList(getType(entity)))
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

}

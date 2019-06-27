package uk.ac.ebi.uniprot.api.rest.validation;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is the solr query validator that is responsible to verify if the query has.
 * - valid field names
 * - valid query field type
 * - if applicable, expected value format, for example, boolean, numbers, valid accession, etc...
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidSolrQueryFields.QueryFieldValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrQueryFields {

    Class<? extends Enum<? extends SearchField>> fieldValidatorClazz();

    String messagePrefix();

    String message() default "{search.uniprot.invalid.query.field}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class QueryFieldValidator implements ConstraintValidator<ValidSolrQueryFields, String> {

        private static final Logger LOGGER = getLogger(QueryFieldValidator.class);
        private static final String DEFAULT_FIELD_NAME = "default_field";
        private List<SearchField> fieldList;
        private String messagePrefix;

        @Override
        public void initialize(ValidSolrQueryFields constraintAnnotation) {
            try {
                fieldList = new ArrayList<>();
                Class<? extends Enum<? extends SearchField>> enumClass = constraintAnnotation.fieldValidatorClazz();

                Enum[] enumValArr = enumClass.getEnumConstants();

                for (Enum enumVal : enumValArr) {
                    fieldList.add((SearchField) enumVal);
                }
                messagePrefix = constraintAnnotation.messagePrefix();
            } catch (Exception e) {
                LOGGER.error("Error initializing QueryFieldValidator", e);
            }
        }

        @Override
        public boolean isValid(String queryString, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notEmpty(queryString)) {
                try {
                    StandardQueryParser qp = new StandardQueryParser();
                    qp.setAllowLeadingWildcard(true);
                    Query query = qp.parse(queryString, DEFAULT_FIELD_NAME);
                    if (!(query instanceof MatchAllDocsQuery)) {
                        isValid = hasValidateQueryField(query, context);
                    }
                    if (!isValid) {
                        context.disableDefaultConstraintViolation();
                    }
                } catch (Exception e) {
                    //Syntax error is validated by ValidSolrQuerySyntax
                }
            }
            return isValid;
        }

        public void addFieldValueErrorMessage(String fieldName, String value, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.field.value." + fieldName + "}";
            contextImpl.addMessageParameter("0", fieldName);
            contextImpl.addMessageParameter("1", value);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addFieldTypeErrorMessage(String fieldName, SearchFieldType type, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.field.type}";
            String expectedFieldType = getFieldByName(fieldName).getSearchFieldType().name().toLowerCase();
            contextImpl.addMessageParameter("0", fieldName);
            contextImpl.addMessageParameter("1", type.name().toLowerCase());
            contextImpl.addMessageParameter("2", expectedFieldType);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addFieldNameErrorMessage(String fieldName, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.field}";
            contextImpl.addMessageParameter("0", fieldName);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addQueryTypeErrorMessage(Query inputQuery, ConstraintValidatorContext context) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.type}";
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            contextImpl.addMessageParameter("0", inputQuery.getClass().getName());
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        private boolean hasValidateQueryField(Query inputQuery, ConstraintValidatorContext context) {
            boolean validField = true;
            if (inputQuery instanceof TermQuery) {
                TermQuery termQuery = (TermQuery) inputQuery;
                String fieldName = termQuery.getTerm().field();
                String value = termQuery.getTerm().text();
                validField = isValidField(context, fieldName, SearchFieldType.TERM, value);
            } else if (inputQuery instanceof WildcardQuery) {
                WildcardQuery wildcardQuery = (WildcardQuery) inputQuery;
                String fieldName = wildcardQuery.getTerm().field();
                String value = wildcardQuery.getTerm().text();
                validField = isValidField(context, fieldName, SearchFieldType.TERM, value);
            } else if (inputQuery instanceof TermRangeQuery) {
                TermRangeQuery rangeQuery = (TermRangeQuery) inputQuery;
                String fieldName = rangeQuery.getField();
                String value = rangeQuery.toString("");
                validField = isValidField(context, fieldName, SearchFieldType.RANGE, value);
            } else if (inputQuery instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) inputQuery;
                String fieldName = phraseQuery.getTerms()[0].field();
                String value = Arrays.stream(phraseQuery.getTerms()).map(Term::text).collect(Collectors.joining(" "));
                validField = isValidField(context, fieldName, SearchFieldType.TERM, value);
            } else if (inputQuery instanceof BooleanQuery) {
                BooleanQuery booleanQuery = (BooleanQuery) inputQuery;
                for (BooleanClause clause : booleanQuery.clauses()) {
                    if (!hasValidateQueryField(clause.getQuery(), context)) {
                        validField = false;
                    }
                }
            } else {
                addQueryTypeErrorMessage(inputQuery, context);
                validField = false;
            }
            return validField;
        }

        private boolean isValidField(ConstraintValidatorContext context, String fieldName, SearchFieldType type, String value) {
            boolean validField = true;
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            SearchField searchField = getFieldByName(fieldName);
            if (searchField == null && !fieldName.equals(DEFAULT_FIELD_NAME)) {
                addFieldNameErrorMessage(fieldName, contextImpl);
                validField = false;
            } else if (!searchField.getSearchFieldType().equals(type)) {
                addFieldTypeErrorMessage(fieldName, type, contextImpl);
                validField = false;
            } else if (!searchField.hasValidValue(value)) {
                addFieldValueErrorMessage(fieldName, value, contextImpl);
                validField = false;
            }

            return validField;
        }

        private SearchField getFieldByName(String fieldName) {
            return fieldList.stream()
                    .filter(searchField -> searchField.getName().equalsIgnoreCase(fieldName))
                    .findFirst().orElse(null);
        }
    }
}

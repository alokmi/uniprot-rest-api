package uk.ac.ebi.uniprot.rest.validation.error;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import uk.ac.ebi.uniprot.common.exception.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author lgonzales
 */
class ResponseExceptionHandlerTest {

    public static final String REQUEST_URL = "http://localhost/test";
    private static ResponseExceptionHandler errorHandler;

    @BeforeAll
    static void setUp(){
        ErrorHandlerConfig config = new ErrorHandlerConfig();
        MessageSource messageSource = config.messageSource();
        errorHandler = new ResponseExceptionHandler(messageSource);

    }

    @Test
    void handleInternalServerErrorWithDebug() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getParameter("debugError")).thenReturn("true");
        NullPointerException causedBy = new NullPointerException("Null Pointer");
        Throwable error = new Throwable("Error Message",causedBy);

        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler.handleInternalServerError(error,request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1,responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON,responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL,errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(2,errorMessage.getMessages().size());

        assertEquals("Internal server error",errorMessage.getMessages().get(0));
        assertEquals("Caused by: Null Pointer",errorMessage.getMessages().get(1));
    }

    @Test
    void handleBindExceptionBadRequest() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        BindException error = new BindException("target","objectName");
        error.addError(new FieldError("objectName1","field1","Error With field 1"));
        error.addError(new FieldError("objectName2","field2","Error With field 2"));


        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler.handleBindExceptionBadRequest(error,request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1,responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON,responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL,errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(2,errorMessage.getMessages().size());

        assertEquals("Error With field 1",errorMessage.getMessages().get(0));
        assertEquals("Error With field 2",errorMessage.getMessages().get(1));
    }

    @Test
    void constraintViolationException() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();
        constraintViolations.add(ConstraintViolationImpl.forBeanValidation("",null,
                null,"Error Message",null,null,null,null,null,null,null,null));
        ConstraintViolationException error = new ConstraintViolationException(constraintViolations);


        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler.constraintViolationException(error,request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1,responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_JSON,responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL,errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1,errorMessage.getMessages().size());

        assertEquals("Error Message",errorMessage.getMessages().get(0));




    }

    @Test
    void noHandlerFoundExceptionForXMLContentType() {
        //when
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
        Mockito.when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_XML_VALUE);
        ResourceNotFoundException error = new ResourceNotFoundException("Error Message");

        ResponseEntity<ResponseExceptionHandler.ErrorInfo> responseEntity = errorHandler.noHandlerFoundException(error,request);

        //then
        assertNotNull(responseEntity);
        assertNotNull(responseEntity.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,responseEntity.getStatusCode());

        assertNotNull(responseEntity.getHeaders());
        assertEquals(1,responseEntity.getHeaders().size());
        assertEquals(MediaType.APPLICATION_XML,responseEntity.getHeaders().getContentType());

        assertNotNull(responseEntity.getBody());
        ResponseExceptionHandler.ErrorInfo errorMessage = responseEntity.getBody();

        assertEquals(REQUEST_URL,errorMessage.getUrl());

        assertNotNull(errorMessage.getMessages());
        assertEquals(1,errorMessage.getMessages().size());

        assertEquals("Resource not found",errorMessage.getMessages().get(0));
    }

}
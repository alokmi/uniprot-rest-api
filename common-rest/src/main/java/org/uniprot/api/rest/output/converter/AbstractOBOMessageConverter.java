package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

public abstract class AbstractOBOMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private final OBOFormatWriter oboFormatWriter;

    public AbstractOBOMessageConverter(Class<T> messageConverterEntryClass) {
        super(UniProtMediaType.OBO_MEDIA_TYPE, messageConverterEntryClass);
        this.oboFormatWriter = new OBOFormatWriter();
    }

    protected abstract Frame getTermFrame(T entity);

    protected abstract String getHeaderNamespace();

    public Frame getHeaderFrame() {
        Frame headerFrame = new Frame(Frame.FrameType.HEADER);
        headerFrame.addClause(
                new Clause(OBOFormatConstants.OboFormatTag.TAG_FORMAT_VERSION, "1.2"));
        headerFrame.addClause(
                new Clause(
                        OBOFormatConstants.OboFormatTag.TAG_DATE,
                        OBOFormatConstants.headerDateFormat().format(new Date())));
        headerFrame.addClause(
                new Clause(
                        OBOFormatConstants.OboFormatTag.TAG_DEFAULT_NAMESPACE,
                        getHeaderNamespace()));
        return headerFrame;
    }

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        Frame headerFrame = getHeaderFrame();
        StringWriter out = new StringWriter();
        this.oboFormatWriter.writeHeader(headerFrame, new PrintWriter(out), null);
        outputStream.write(out.getBuffer().toString().getBytes());
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        Frame termFrame = getTermFrame(entity);
        StringWriter out = new StringWriter();
        this.oboFormatWriter.write(termFrame, new PrintWriter(out), null);
        outputStream.write(out.getBuffer().toString().getBytes());
    }
}

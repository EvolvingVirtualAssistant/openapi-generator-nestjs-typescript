package com.eva.codegen.lambda;

import com.samskivert.mustache.Mustache.Lambda;
import com.samskivert.mustache.Template.Fragment;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import org.openapitools.codegen.CodegenConfig;

public class NestJsPathResolveLambda implements Lambda {

    private CodegenConfig generator = null;

    public NestJsPathResolveLambda() {
    }

    public NestJsPathResolveLambda generator(CodegenConfig generator) {
        this.generator = generator;
        return this;
    }

    @Override
    public void execute(Fragment fragment, Writer writer) throws IOException {
        String text = fragment.execute();
        if (this.generator != null && this.generator.reservedWords().contains(text)) {
            text = this.generator.escapeReservedWord(text);
        }

        //expected format: x-path:...,x-baseName:...
        int baseNameIdx = text.indexOf(",x-baseName:");
        String path = text.substring(7, baseNameIdx);

        path = path.replaceAll("\\{", ":").replaceAll("}", "");

        String baseName = text.substring(baseNameIdx + 12).toLowerCase(Locale.ROOT).trim();
        if(!baseName.isEmpty() && path.indexOf("/"+baseName) == 0) {
            path = path.substring(baseName.length() + 1);
        }

        if(!path.isEmpty()) {
            path = path.indexOf("/") == 0 ? path.substring(1) : path;
            path = "'" + path + "'";
        }
        writer.write(path);
    }
}

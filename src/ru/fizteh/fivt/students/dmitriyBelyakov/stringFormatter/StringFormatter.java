package ru.fizteh.fivt.students.dmitriyBelyakov.stringFormatter;

import ru.fizteh.fivt.format.FormatterException;

import java.util.ArrayList;

public class StringFormatter implements ru.fizteh.fivt.format.StringFormatter {
    ArrayList<StringFormatterExtension> extensions;

    StringFormatter() {
        extensions = new ArrayList<>();
    }

    public void addExtension(StringFormatterExtension extension) throws FormatterException {
        if (extension == null) {
            throw new FormatterException("Null pointer.");
        }
        try {
            extensions.add(extension);
        } catch (Throwable t) {
            throw new FormatterException(t.getMessage());
        }
    }

    public boolean supported(Class<?> clazz) {
        for(StringFormatterExtension extension: extensions) {
            if(extension.supports(clazz)) {
                return true;
            }
        }
        return false;
    }

    public StringFormatterExtension getExtension(Object object) {
        for(StringFormatterExtension extension: extensions) {
            if(extension.supports(object.getClass())) {
                return extension;
            }
        }
        throw new FormatterException("Extension not find.");
    }

    private void formatWithArray(StringBuilder buffer, String format, Object[] args) throws FormatterException {
        try {
            boolean isArgument = false;
            boolean objectGet = false;
            boolean field = false;
            boolean pattern = true;
            int numOfObjectPosition = 0;
            int numOfFieldPosition = 0;
            int numOfPatternPosition = 0;
            Object object = null;
            for (int i = 0; i < format.length(); ++i) {
                char c = format.charAt(i);
                if (!isArgument) {
                    if (c == '{') {
                        if (format.charAt(i + 1) == '{') {
                            buffer.append('{');
                            ++i;
                        } else {
                            isArgument = true;
                            numOfObjectPosition = i + 1;
                        }
                    } else if (c == '}') {
                        if (format.charAt(i + 1) == '}') {
                            buffer.append('}');
                            ++i;
                        } else {
                            throw new FormatterException("Incorrect format.");
                        }
                    } else {
                        buffer.append(c);
                    }
                } else {
                    if (!objectGet) {
                        if (c == '.' || c == ':' || c == '}') {
                            try {
                                int argumentNumber = Integer.parseInt(format.substring(numOfObjectPosition, i));
                                object = args[argumentNumber];
                                objectGet = true;
                                if (c == '.') {
                                    field = true;
                                    numOfFieldPosition = i + 1;
                                } else if (c == ':') {
                                    pattern = true;
                                    numOfPatternPosition = i + 1;
                                } else {
                                    buffer.append(object.toString());
                                    isArgument = false;
                                    objectGet = false;
                                    pattern = false;
                                    field = false;
                                }
                            } catch (Throwable t) {
                                throw new FormatterException(t.getMessage());
                            }
                        } else if (!Character.isDigit(c)) {
                            throw new FormatterException("Incorrect format.");
                        }
                    } else {
                        if (field) {
                            if (c == '.' || c == ':' || c == '}') {
                                object = object.getClass().getDeclaredField(format.substring(numOfFieldPosition, i)).get(object);
                                if(object == null) {
                                    throw new FormatterException("Null pointer field.");
                                }
                                if(c == ':') {
                                    field = false;
                                    pattern = true;
                                    numOfPatternPosition = i + 1;
                                } else if(c == '}') {
                                    buffer.append(object.toString());
                                    isArgument = false;
                                    objectGet = false;
                                    pattern = false;
                                    field = false;
                                } else if(c == '.') {
                                    numOfFieldPosition = i + 1;
                                }
                            }
                        } else if (pattern) {
                            if (c == '}') {
                                if (!supported(object.getClass())) {
                                    throw new RuntimeException("Type doesn't supported.");
                                }
                                getExtension(object).format(buffer, object, format.substring(numOfPatternPosition, i));
                                isArgument = false;
                                objectGet = false;
                                pattern = false;
                                field = false;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            //System.out.println(t.getClass().getName());
            throw new FormatterException(t.getMessage());
        }
    }

    @Override
    public String format(String format, Object... args) throws FormatterException {
        StringBuilder builder = new StringBuilder();
        format(builder, format, args);
        return builder.toString();
    }

    @Override
    public void format(StringBuilder buffer, String format, Object... args) {
        formatWithArray(buffer, format, args);
    }
}
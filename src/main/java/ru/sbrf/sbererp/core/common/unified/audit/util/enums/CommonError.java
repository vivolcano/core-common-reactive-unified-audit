package ru.sbrf.sbererp.core.common.unified.audit.util.enums;

/**
 * Интерфейс для определения общих ошибок.
 * <p>
 * Этот интерфейс используется для предоставления кода ошибки и сообщения об ошибке.
 */
public interface CommonError {

    /**
     * Возвращает код ошибки.
     *
     * @return код ошибки.
     */
    String getCode();

    /**
     * Возвращает сообщение об ошибке.
     *
     * @return сообщение об ошибке.
     */
    String getMessage();
}

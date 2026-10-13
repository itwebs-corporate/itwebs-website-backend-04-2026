package ru.itwebs.cms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({ru.itwebs.cms.blog.BlogConflictException.class,
            org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.OptimisticLockingFailureException.class})
    ProblemDetail conflict(RuntimeException error) {
        String message = error instanceof ru.itwebs.cms.blog.BlogConflictException
                ? error.getMessage() : "Конфликт данных: запись изменена, slug занят или запись используется";
        return problem(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException error) {
        return problem(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException error) {
        return problem(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return problem(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody(org.springframework.http.converter.HttpMessageNotReadableException error) {
        return problem(HttpStatus.BAD_REQUEST, "Некорректный JSON: проверьте типы полей и формат дат");
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ProblemDetail invalidParameter(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException error) {
        return problem(HttpStatus.BAD_REQUEST, "Некорректный параметр: " + error.getName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException error) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Файл превышает допустимый размер 15 МБ");
    }

    private ProblemDetail problem(HttpStatus status, String message) {
        ProblemDetail result = ProblemDetail.forStatus(status);
        result.setDetail(message);
        return result;
    }
}

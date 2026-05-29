package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Comment;

import java.util.List;

public interface CommentService {
    // ulozi novy komentar
    void addComment(Comment comment) throws CommentException;

    // vrati posledne komentare pre konkretnu hru alebo level
    List<Comment> getComments(String game) throws CommentException;

    // vymaze komentare, pouziva sa hlavne v testoch
    void reset() throws CommentException;
}

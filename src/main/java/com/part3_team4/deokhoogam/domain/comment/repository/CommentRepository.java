package com.part3_team4.deokhoogam.domain.comment.repository;

import com.part3_team4.deokhoogam.domain.comment.entity.Comment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

}

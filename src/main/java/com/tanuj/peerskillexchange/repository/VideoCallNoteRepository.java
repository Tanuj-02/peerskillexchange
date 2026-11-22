package com.tanuj.peerskillexchange.repository;

import com.tanuj.peerskillexchange.model.VideoCallNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VideoCallNoteRepository extends JpaRepository<VideoCallNote, Long> {
    List<VideoCallNote> findTop40ByCallIdOrderByIdAsc(Long callId);
    boolean existsByCallIdAndAuthorIdAndRequestId(Long callId, String authorId, String requestId);
}

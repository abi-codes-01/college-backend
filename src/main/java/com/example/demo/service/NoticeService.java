package com.example.demo.service;

import com.example.demo.model.Notice;
import com.example.demo.repository.NoticeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByIdDesc();
    }

    public List<Notice> getPublicNotices() {
        return noticeRepository.findAllByOrderByIdDesc();
    }

    public Notice createNotice(Notice notice) {
        if (notice.getDate() == null) {
            notice.setDate(LocalDate.now());
        }
        if (notice.getIsPublic() == null) {
            notice.setIsPublic(true);
        }
        return noticeRepository.save(notice);
    }

    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}

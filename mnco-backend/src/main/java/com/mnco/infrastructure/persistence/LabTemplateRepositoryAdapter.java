package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.LabTemplateMapper;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.domain.repository.LabTemplateRepository;
import com.mnco.infrastructure.persistence.repository.LabTemplateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LabTemplateRepositoryAdapter implements LabTemplateRepository {

    private final LabTemplateJpaRepository jpaRepository;
    private final LabTemplateMapper mapper;

    @Override
    public LabTemplate save(LabTemplate template) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(template)));
    }

    @Override
    public Optional<LabTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<LabTemplate> findAllPublic() {
        return jpaRepository.findByIsPublicTrue().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<LabTemplate> findByAuthorId(UUID authorId) {
        return jpaRepository.findByAuthorId(authorId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}

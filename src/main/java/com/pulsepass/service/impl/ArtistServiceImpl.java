package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.ArtistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository repository;
    private final ArtistMapper mapper;

    public ArtistServiceImpl(ArtistRepository repository, ArtistMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ArtistResponse findById(Long id) {
        // BR-ARTIST-001
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Artist with id " + id + " not found"
                ));
    }

    @Override
    public ArtistResponse findByStageName(String stageName) {
        return repository.findByStageNameIgnoreCase(stageName)
                .map(mapper::toResponse)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Artist with stage name " + stageName + " not found"
                ));
    }

    @Override
    public List<ArtistResponse> findActiveArtists() {
        // BR-ARTIST-002
        List<Artist> artists = repository
                .findByActiveTrueOrderByStageNameAsc();
        return artists.stream()
                .map(mapper::toResponse)
                .toList();
    }
}

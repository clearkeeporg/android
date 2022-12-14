package com.clearkeep.domain.usecase.server

import com.clearkeep.domain.repository.ServerRepository
import javax.inject.Inject

class GetDefaultServerProfileAsStateUseCase @Inject constructor(private val serverRepository: ServerRepository) {
    operator fun invoke() = serverRepository.getDefaultServerProfileAsState()
}
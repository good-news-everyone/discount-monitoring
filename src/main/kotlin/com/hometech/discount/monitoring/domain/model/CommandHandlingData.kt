package com.hometech.discount.monitoring.domain.model

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard

class CommandHandlingData(val message: String, val replyKeyboard: ReplyKeyboard? = null)

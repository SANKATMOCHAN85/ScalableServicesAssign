package com.bits.scalableservices.dao

import java.time.LocalDate

case class TicketDetails
(
  From: String,
  destination: String,
  dateOfBooking: String,
  dateOfTravel: String,
  passengers: String,
  vehicleName: String,
  ticketNumber: String,
  var userId: Int
)

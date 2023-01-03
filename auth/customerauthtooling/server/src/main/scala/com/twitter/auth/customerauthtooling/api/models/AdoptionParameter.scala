package com.twitter.auth.customerauthtooling.api.models

class AdoptionParameter

object AdoptionParameter {
  final class FoundInTfe extends AdoptionParameter

  final class IsInternalEndpoint extends AdoptionParameter

  final class IsNgRoute extends AdoptionParameter

  final class RequiresAuth extends AdoptionParameter

  final class IsAppOnlyOrGuest extends AdoptionParameter

  final class Oauth1OrSession extends AdoptionParameter

  final class AlreadyAdoptedDps extends AdoptionParameter
}

package lxx.waves

import java.util.ArrayList
import lxx.model.LxxWave
import lxx.model.BattleState
import robocode.util.Utils
import robocode.Bullet
import java.lang.Math.abs
import lxx.model.LxxPoint
import lxx.math.*

class WavesWatcher(private val attackerName: String) {

    var wavesInAir: ArrayList<LxxWave> = ArrayList<LxxWave>()

    public fun watch(wave: LxxWave) {
        wavesInAir += wave
    }

    public fun getBrokenWaves(battleState: BattleState): List<BrokenWave> {
        val (broken, inAir) = wavesInAir.partition {
            it.travelledDistance(battleState.time) >= it.attacker.distance(battleState.robotByName(it.victim.name))
        }

        wavesInAir = inAir as ArrayList<LxxWave>

        return broken.map {
            BrokenWave(it, it.toBearingOffset(it.attacker.angleTo(battleState.robotByName(it.victim.name))))
        }
    }

    public fun getHitWaves(battleState: BattleState): List<BrokenWave> {
        var brokenWaves = ArrayList<BrokenWave>()
        for (detectedBullet in battleState.detectedBullets.get(attackerName)!!) {
            var wave =
                    findWave(battleState.time, detectedBullet) ?:
                    generateWave(battleState, detectedBullet)
            brokenWaves += BrokenWave(wave, wave.toBearingOffset(detectedBullet.getHeadingRadians()))
        }

        return brokenWaves
    }

    private fun findWave(time: Long, bullet: Bullet) = wavesInAir.find {
        Utils.isNear(it.speed, bullet.getVelocity()) &&
        abs(it.travelledDistance(time) - it.attacker.distance(bullet.getX(), bullet.getY())) < bullet.getVelocity()
    }

    private fun generateWave(battleState: BattleState, bullet: Bullet): LxxWave {
        val bulletSpeed = bullet.getVelocity()
        val bulletReverseHeading = Utils.normalAbsoluteAngle(bullet.getHeadingRadians() + RADIANS_180)

        var attacker = battleState.robotByName(bullet.getName()!!)
        var victim = battleState.robotByName(bullet.getVictim()!!)
        var bulletPos = LxxPoint(bullet.getX(), bullet.getY())

        while (attacker.prevState != null &&
               attacker.prevState!!.distance(bulletPos.project(bulletReverseHeading, bulletSpeed)) < attacker.distance(bulletPos)) {
            attacker = attacker.prevState!!
            victim = victim.prevState!!
            bulletPos = bulletPos.project(bulletReverseHeading, bulletSpeed)
        }

        assert(attacker.time == victim.time, "Attker: $attacker, victim: $victim")

        return LxxWave(attacker.time, attacker, victim, bulletSpeed)
    }

}

data class BrokenWave(val wave: LxxWave, val hitOffset: Double)
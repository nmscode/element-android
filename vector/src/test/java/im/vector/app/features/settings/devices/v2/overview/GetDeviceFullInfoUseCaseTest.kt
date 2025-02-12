/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.devices.v2.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import im.vector.app.features.settings.devices.v2.CurrentSessionCrossSigningInfo
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.GetCurrentSessionCrossSigningInfoUseCase
import im.vector.app.features.settings.devices.v2.GetEncryptionTrustLevelForDeviceUseCase
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.Optional

private const val A_DEVICE_ID = "device-id"
private const val A_TIMESTAMP = 123L

class GetDeviceFullInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val getCurrentSessionCrossSigningInfoUseCase = mockk<GetCurrentSessionCrossSigningInfoUseCase>()
    private val getEncryptionTrustLevelForDeviceUseCase = mockk<GetEncryptionTrustLevelForDeviceUseCase>()
    private val checkIfSessionIsInactiveUseCase = mockk<CheckIfSessionIsInactiveUseCase>()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val getDeviceFullInfoUseCase = GetDeviceFullInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance,
            getCurrentSessionCrossSigningInfoUseCase = getCurrentSessionCrossSigningInfoUseCase,
            getEncryptionTrustLevelForDeviceUseCase = getEncryptionTrustLevelForDeviceUseCase,
            checkIfSessionIsInactiveUseCase = checkIfSessionIsInactiveUseCase,
    )

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given current session and info for device when getting device info then the result is correct`() = runTest {
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo()
        val deviceInfo = DeviceInfo(
                lastSeenTs = A_TIMESTAMP
        )
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData = MutableLiveData(Optional(deviceInfo))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData.givenAsFlow()
        val cryptoDeviceInfo = CryptoDeviceInfo(deviceId = A_DEVICE_ID, userId = "")
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(cryptoDeviceInfo))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()
        val trustLevel = givenTrustLevel(currentSessionCrossSigningInfo, cryptoDeviceInfo)
        val isInactive = false
        every { checkIfSessionIsInactiveUseCase.execute(any()) } returns isInactive

        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        deviceFullInfo shouldBeEqualTo Optional(
                DeviceFullInfo(
                        deviceInfo = deviceInfo,
                        cryptoDeviceInfo = cryptoDeviceInfo,
                        roomEncryptionTrustLevel = trustLevel,
                        isInactive = isInactive,
                )
        )
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
        verify { getCurrentSessionCrossSigningInfoUseCase.execute() }
        verify { getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo) }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow() }
        verify { checkIfSessionIsInactiveUseCase.execute(A_TIMESTAMP) }
    }

    @Test
    fun `given current session and no info for device when getting device info then the result is null`() = runTest {
        givenCurrentSessionCrossSigningInfo()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData.givenAsFlow()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()

        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        deviceFullInfo shouldBeEqualTo Optional(null)
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow() }
    }

    @Test
    fun `given no current session when getting device info then the result is empty`() = runTest {
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        deviceFullInfo shouldBeEqualTo null
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
    }

    private fun givenCurrentSessionCrossSigningInfo(): CurrentSessionCrossSigningInfo {
        val currentSessionCrossSigningInfo = CurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = true,
                isCrossSigningVerified = false
        )
        every { getCurrentSessionCrossSigningInfoUseCase.execute() } returns flowOf(currentSessionCrossSigningInfo)
        return currentSessionCrossSigningInfo
    }

    private fun givenTrustLevel(currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo, cryptoDeviceInfo: CryptoDeviceInfo?): RoomEncryptionTrustLevel {
        val trustLevel = RoomEncryptionTrustLevel.Trusted
        every { getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo) } returns trustLevel
        return trustLevel
    }
}

<script setup lang="ts">
const { isLoggedIn, login, register, logout, accessToken } = useAuth()

const tab = ref(0) // 0 = login, 1 = register
const tabLabel = computed(() => tab.value === 0 ? 'login' : 'register')
const email = ref('')
const password = ref('')
const status = ref('')
const error = ref('')

async function submit() {
  status.value = ''
  error.value = ''
  try {
    if (tabLabel.value === 'login') {
      await login(email.value, password.value)
      status.value = 'Logged in successfully.'
    } else {
      await register(email.value, password.value)
      status.value = 'Registered — now log in.'
      tab.value = 0
    }
  } catch (e: any) {
    error.value = e?.data?.message ?? e?.message ?? 'Unknown error'
  }
}

// ── Device panel ──────────────────────────────────────────────
const devices = ref<any[]>([])
const deviceError = ref('')

const API = 'http://localhost:8080'

async function loadDevices() {
  deviceError.value = ''
  try {
    devices.value = await $fetch(`${API}/api/devices`, {
      headers: { Authorization: `Bearer ${accessToken.value}` }
    })
  } catch (e: any) {
    deviceError.value = e?.data?.message ?? e?.message ?? 'Failed to load devices'
  }
}

const newDeviceName = ref('')
const newDevicePort = ref(7070)
const registerResult = ref<any>(null)

async function registerDevice() {
  registerResult.value = null
  deviceError.value = ''
  try {
    registerResult.value = await $fetch(`${API}/api/devices/register`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${accessToken.value}` },
      body: { name: newDeviceName.value, streamPort: newDevicePort.value }
    })
    await loadDevices()
  } catch (e: any) {
    deviceError.value = e?.data?.message ?? e?.message ?? 'Failed to register device'
  }
}

async function deleteDevice(id: string) {
  deviceError.value = ''
  try {
    await $fetch(`${API}/api/devices/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${accessToken.value}` }
    })
    await loadDevices()
  } catch (e: any) {
    deviceError.value = e?.data?.message ?? e?.message ?? 'Failed to delete device'
  }
}

watch(isLoggedIn, (v) => { if (v) loadDevices() })
onMounted(() => { if (isLoggedIn.value) loadDevices() })
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-950 p-8 max-w-2xl mx-auto space-y-8">
    <h1 class="text-2xl font-bold">SmartQuery API Tester</h1>

    <!-- Auth card -->
    <UCard v-if="!isLoggedIn">
      <template #header>
        <UTabs
          :items="[{ label: 'Login' }, { label: 'Register' }]"
          v-model="tab"
        />
      </template>

      <div class="space-y-4">
        <UFormGroup label="Email">
          <UInput v-model="email" type="email" placeholder="dev@smartquery.local" />
        </UFormGroup>
        <UFormGroup label="Password">
          <UInput v-model="password" type="password" placeholder="devpassword" />
        </UFormGroup>

        <UButton block @click="submit">
          {{ tabLabel === 'login' ? 'Login' : 'Register' }}
        </UButton>

        <UAlert v-if="status" color="success" :description="status" />
        <UAlert v-if="error" color="error" :description="error" />
      </div>
    </UCard>

    <!-- Logged-in panel -->
    <template v-else>
      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">Authenticated</span>
            <UButton size="xs" color="neutral" variant="ghost" @click="logout">Logout</UButton>
          </div>
        </template>
        <p class="text-xs font-mono break-all text-gray-500">{{ accessToken }}</p>
      </UCard>

      <UAlert v-if="status" color="success" :description="status" />
      <UAlert v-if="error" color="error" :description="error" />

      <!-- Devices -->
      <UCard>
        <template #header>
          <span class="font-semibold">Devices</span>
        </template>

        <div class="space-y-4">
          <!-- Register new device -->
          <div class="flex gap-2">
            <UInput v-model="newDeviceName" placeholder="Device name" class="flex-1" />
            <UInput v-model="newDevicePort" type="number" placeholder="Port" class="w-24" />
            <UButton @click="registerDevice">Register</UButton>
          </div>

          <!-- New API key (shown once) -->
          <UAlert
            v-if="registerResult"
            color="warning"
            title="Save this API key — shown once!"
            :description="`deviceId: ${registerResult.deviceId}  |  apiKey: ${registerResult.apiKey}`"
          />

          <UAlert v-if="deviceError" color="error" :description="deviceError" />

          <!-- Device list -->
          <div v-if="devices.length === 0" class="text-sm text-gray-400">No devices registered.</div>
          <div v-for="d in devices" :key="d.id" class="flex items-center justify-between p-2 rounded border dark:border-gray-700">
            <div>
              <p class="font-medium">{{ d.name }}</p>
              <p class="text-xs text-gray-400">{{ d.id }} · port {{ d.streamPort }}</p>
            </div>
            <UButton size="xs" color="error" variant="ghost" @click="deleteDevice(d.id)">Delete</UButton>
          </div>
        </div>
      </UCard>
    </template>
  </div>
</template>

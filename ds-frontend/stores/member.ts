import { defineStore } from 'pinia'

interface Member {
  id: number | null
  memberNo: string
  phone: string
  realName: string
  email: string
  emailVerified: number
  phoneVerified: number
  nickname: string
  avatar: string
  levelId: number | null
  levelName: string
  status: number
  walletBalance: number
  frozenAmount: number
  totalPv: number
  teamCount: number
  monthBonus: number
  wechatBound: number
  workWechatBound: number
  createTime: string
  [key: string]: any
}

interface MemberState {
  member: Member
  teamTree: any[]
}

export const useMemberStore = defineStore('member', {
  state: (): MemberState => ({
    member: {
        id: null,
        memberNo: '',
        phone: '',
        realName: '',
        email: '',
        emailVerified: 0,
        phoneVerified: 0,
        nickname: '',
        avatar: '',
        levelId: null,
        levelName: '',
        status: 1,
        walletBalance: 0,
        frozenAmount: 0,
        totalPv: 0,
        teamCount: 0,
        monthBonus: 0,
        wechatBound: 0,
        workWechatBound: 0,
        createTime: '',
      },
    teamTree: [],
  }),

  getters: {
    currentMember: (state) => state.member,
    downlineTree: (state) => state.teamTree,
  },

  actions: {
    async fetchMember(memberId?: number) {
      const { get } = useApi()
      const url = memberId ? `/api/member/${memberId}` : '/api/member/info'
      const res: any = await get(url)
      this.member = res.data
    },

    async fetchTeamTree(memberId?: number) {
      const { get } = useApi()
      const id = memberId || this.member.id
      if (!id) {
        this.teamTree = []
        return
      }
      const res: any = await get(`/api/member/${id}/team`)
      this.teamTree = res.data || []
    },

    reset() {
      this.member = {
        id: null,
        memberNo: '',
        phone: '',
        realName: '',
        email: '',
        emailVerified: 0,
        phoneVerified: 0,
        nickname: '',
        avatar: '',
        levelId: null,
        levelName: '',
        status: 1,
        walletBalance: 0,
        frozenAmount: 0,
        totalPv: 0,
        teamCount: 0,
        monthBonus: 0,
        wechatBound: 0,
        workWechatBound: 0,
        createTime: '',
      }
      this.teamTree = []
    },
  },
})
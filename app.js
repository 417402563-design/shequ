/* 智慧社区数字化租赁服务平台 - Vue3 前端入口 */
const { createApp } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

const ROLE_LABELS = { AGENT: '中介', LANDLORD: '房东', TENANT: '租客' };

createApp({
    data() {
        return {
            currentUser: JSON.parse(localStorage.getItem('shequ_user') || 'null'),
            loginForm: { username: 'agent1', password: '123456' },
            loginLoading: false,
            page: 'stats',
            notifications: [],
            unreadCount: 0,
            // 统计
            overview: {},
            areaStats: [],
            // 用户
            users: [], landlordUsers: [],
            userQuery: { role: '', keyword: '' },
            userDialog: false,
            userForm: {},
            // 房源
            houses: [], areas: [],
            houseQuery: { status: '', area: '', keyword: '' },
            houseDialog: false,
            houseForm: {},
            // 租客
            tenants: [],
            tenantQuery: { keyword: '' },
            tenantDialog: false, tenantForm: {},
            viewingDialog: false, viewingForm: {},
            viewingListDialog: false, viewingList: [], currentTenant: {},
            // 合同
            contracts: [], expiringContracts: [],
            contractQuery: { status: '', keyword: '' },
            contractDialog: false, contractForm: {},
            contentDialog: false, contractContent: '',
            terminateDialog: false, terminateRow: null, depositHandle: 'REFUNDED',
            // 费用
            bills: [],
            billQuery: { status: '', type: '' },
            billDialog: false, billForm: {},
            charts: {}
        };
    },
    computed: {
        pageTitle() {
            return { stats: '统计分析', users: '用户管理', houses: '房源管理',
                tenants: '租客管理', contracts: '合同管理', bills: '费用管理' }[this.page];
        },
        statCards() {
            const o = this.overview;
            return [
                { label: '房源总数', value: o.houseTotal ?? '-', color: '#409EFF' },
                { label: '出租率', value: (o.occupancyRate ?? 0) + '%', color: '#67C23A' },
                { label: '本月租金收入', value: '￥' + this.money(o.monthIncome), color: '#E6A23C' },
                { label: '累计租金收入', value: '￥' + this.money(o.totalIncome), color: '#F56C6C' },
                { label: '生效合同', value: o.signedContracts ?? '-', color: '#409EFF' },
                { label: '待缴账单', value: o.unpaidBills ?? '-', color: '#E6A23C' },
                { label: '租客档案', value: o.tenantTotal ?? '-', color: '#67C23A' },
                { label: '30天内到期合同', value: o.expiringContracts ?? '-', color: '#F56C6C' }
            ];
        },
        signedContracts() {
            return this.contracts.filter(c => c.status === 'SIGNED');
        },
        availableHouses() {
            return this.houses.filter(h => h.status === 'ON');
        },
        unpaidSum() {
            return this.money(this.bills.filter(b => b.status === 'UNPAID')
                .reduce((s, b) => s + Number(b.amount || 0), 0));
        }
    },
    mounted() {
        if (this.currentUser) this.initAll();
        window.addEventListener('resize', () => {
            Object.values(this.charts).forEach(c => c.resize());
        });
    },
    methods: {
        /* ============ 通用请求 ============ */
        async api(url, method = 'GET', body) {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: body ? JSON.stringify(body) : undefined
            });
            const json = await res.json();
            if (json.code !== 0) {
                ElMessage.error(json.msg || '请求失败');
                throw new Error(json.msg);
            }
            return json.data;
        },
        get(u) { return this.api(u); },
        post(u, body) { return this.api(u, 'POST', body || {}); },
        put(u, body) { return this.api(u, 'PUT', body || {}); },
        del(u) { return this.api(u, 'DELETE').then(() => this.reloadCurrent()); },
        reloadCurrent() {
            ({ stats: this.loadStats, users: this.loadUsers, houses: this.loadHouses,
               tenants: this.loadTenants, contracts: this.loadContracts, bills: this.loadBills }[this.page] || (() => {}))();
        },
        money(v) { return v == null ? '0' : Number(v).toLocaleString('zh-CN'); },

        /* ============ 登录/通知 ============ */
        fillAccount(u) { this.loginForm.username = u; this.loginForm.password = '123456'; },
        async doLogin() {
            if (!this.loginForm.username || !this.loginForm.password) {
                return ElMessage.warning('请输入用户名和密码');
            }
            this.loginLoading = true;
            try {
                this.currentUser = await this.post('/api/auth/login', this.loginForm);
                localStorage.setItem('shequ_user', JSON.stringify(this.currentUser));
                ElMessage.success('欢迎，' + this.currentUser.name);
                this.initAll();
            } finally {
                this.loginLoading = false;
            }
        },
        logout() {
            localStorage.removeItem('shequ_user');
            this.currentUser = null;
        },
        async initAll() {
            await Promise.all([this.loadUsers(), this.loadHouses(), this.loadTenants(),
                this.loadContracts(), this.loadBills(), this.loadStats(), this.loadNotif()]);
            this.$nextTick(() => this.renderCharts());
        },
        async loadNotif() {
            if (!this.currentUser) return;
            const target = this.currentUser.name;
            this.notifications = await this.get('/api/notifications?target=' + encodeURIComponent(target));
            const u = await this.get('/api/notifications/unread?target=' + encodeURIComponent(target));
            this.unreadCount = u.count;
        },
        async readAll() {
            await this.put('/api/notifications/read?target=' + encodeURIComponent(this.currentUser.name));
            this.loadNotif();
        },

        /* ============ 页面切换 ============ */
        switchPage(p) {
            this.page = p;
            if (p === 'stats') this.$nextTick(() => { this.loadStats(); this.renderCharts(); });
        },

        /* ============ 统计分析 ============ */
        async loadStats() {
            this.overview = await this.get('/api/stats/overview');
            this.areaStats = await this.get('/api/stats/area');
        },
        async renderCharts() {
            if (this.page !== 'stats') return;
            const [income, status] = await Promise.all([
                this.get('/api/stats/monthly-income'), this.get('/api/stats/house-status')
            ]);
            const mk = (id) => {
                const el = document.getElementById(id);
                if (!el) return null;
                if (!this.charts[id]) this.charts[id] = echarts.init(el);
                return this.charts[id];
            };
            const c1 = mk('incomeChart');
            c1 && c1.setOption({
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: income.map(i => i.month.slice(5) + '月'), axisLabel: { rotate: 45 } },
                yAxis: { type: 'value', name: '元' },
                series: [{ type: 'bar', data: income.map(i => Number(i.income)),
                    itemStyle: { color: '#409EFF' }, label: { show: true, position: 'top' } }],
                grid: { bottom: 60 }
            });
            const c2 = mk('statusChart');
            c2 && c2.setOption({
                tooltip: { trigger: 'item' }, legend: { bottom: 0 },
                series: [{ type: 'pie', radius: ['40%', '68%'], data: status,
                    label: { formatter: '{b}: {c}套 ({d}%)' },
                    color: ['#67C23A', '#E6A23C', '#909399'] }]
            });
            const c3 = mk('areaChart');
            c3 && c3.setOption({
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'value' },
                yAxis: { type: 'category', data: this.areaStats.map(a => a.area).reverse() },
                series: [{ type: 'bar', data: this.areaStats.map(a => a.views).reverse(),
                    itemStyle: { color: '#F56C6C' }, label: { show: true, position: 'right' } }]
            });
        },
        heatPercent(views) {
            const max = Math.max(...this.areaStats.map(a => a.views), 1);
            return Math.round(views * 100 / max);
        },

        /* ============ 用户管理 ============ */
        async loadUsers() {
            const q = this.userQuery;
            this.users = await this.get(`/api/users?role=${q.role || ''}&keyword=${q.keyword || ''}`);
            this.landlordUsers = this.users.filter(u => u.role === 'LANDLORD');
        },
        openUserDialog(row) {
            this.userForm = row ? { ...row, password: '' }
                : { username: '', password: '', name: '', role: 'LANDLORD', phone: '', email: '', status: 1 };
            this.userDialog = true;
        },
        async saveUser() {
            const f = this.userForm;
            if (!f.username || !f.name) return ElMessage.warning('用户名和姓名必填');
            if (f.id) await this.put('/api/users/' + f.id, f);
            else await this.post('/api/users', f);
            this.userDialog = false;
            ElMessage.success('保存成功');
            this.loadUsers();
        },

        /* ============ 房源管理 ============ */
        async loadHouses() {
            const q = this.houseQuery;
            this.houses = await this.get(`/api/houses?status=${q.status || ''}&area=${q.area || ''}&keyword=${q.keyword || ''}`);
            this.areas = await this.get('/api/houses/areas');
        },
        openHouseDialog(row) {
            this.houseForm = row ? { ...row }
                : { title: '', address: '', area: '', layout: '', floor: 1, totalFloors: 10,
                    rent: 2000, deposit: 2000, ownerId: null, ownerName: '', facilities: '', description: '' };
            if (!this.landlordUsers.length) this.loadUsers();
            this.houseDialog = true;
        },
        onOwnerChange(id) {
            const u = this.landlordUsers.find(x => x.id === id);
            this.houseForm.ownerName = u ? u.name : '';
        },
        async saveHouse() {
            const f = this.houseForm;
            if (!f.title || !f.address) return ElMessage.warning('标题和地址必填');
            if (f.id) await this.put('/api/houses/' + f.id, f);
            else await this.post('/api/houses', f);
            this.houseDialog = false;
            ElMessage.success('保存成功');
            this.loadHouses();
        },

        /* ============ 租客管理 ============ */
        async loadTenants() {
            this.tenants = await this.get('/api/tenants?keyword=' + (this.tenantQuery.keyword || ''));
        },
        openTenantDialog(row) {
            this.tenantForm = row ? { ...row }
                : { name: '', gender: '男', idCard: '', phone: '', age: 28, occupation: '', creditScore: 80, remark: '' };
            this.tenantDialog = true;
        },
        async saveTenant() {
            const f = this.tenantForm;
            if (!f.name) return ElMessage.warning('姓名必填');
            if (f.id) await this.put('/api/tenants/' + f.id, f);
            else await this.post('/api/tenants', f);
            this.tenantDialog = false;
            ElMessage.success('保存成功');
            this.loadTenants();
        },
        openViewingDialog(row) {
            this.viewingForm = { tenantId: row.id, tenantName: row.name, houseId: null,
                viewTime: new Date().toISOString().slice(0, 19), feedback: '', intention: 1 };
            if (!this.houses.length) this.loadHouses();
            this.viewingDialog = true;
        },
        async saveViewing() {
            const f = this.viewingForm;
            if (!f.houseId || !f.viewTime) return ElMessage.warning('请选择房源和看房时间');
            await this.post('/api/viewings', f);
            this.viewingDialog = false;
            ElMessage.success('看房记录登记成功');
            this.loadHouses();
        },
        showViewings(row) {
            this.currentTenant = row;
            this.viewingListDialog = true;
            this.loadViewings();
        },
        async loadViewings() {
            this.viewingList = await this.get('/api/tenants/' + this.currentTenant.id + '/viewings');
        },

        /* ============ 合同管理 ============ */
        async loadContracts() {
            const q = this.contractQuery;
            this.contracts = await this.get(`/api/contracts?status=${q.status || ''}&keyword=${q.keyword || ''}`);
            this.expiringContracts = await this.get('/api/contracts/expiring?days=30');
        },
        openContractDialog() {
            if (!this.availableHouses.length) this.loadHouses();
            if (!this.tenants.length) this.loadTenants();
            this.contractForm = { houseId: null, tenantId: null, startDate: this.today(),
                endDate: this.plusMonths(12), payCycle: 1, agentName: this.currentUser.name,
                rent: null, deposit: null };
            this.contractDialog = true;
        },
        onContractHouseChange(id) {
            const h = this.houses.find(x => x.id === id);
            if (h) { this.contractForm.rent = Number(h.rent); this.contractForm.deposit = Number(h.deposit); }
        },
        async saveContract() {
            const f = this.contractForm;
            if (!f.houseId || !f.tenantId || !f.startDate || !f.endDate) return ElMessage.warning('请完整填写合同信息');
            const c = await this.post('/api/contracts', f);
            this.contractDialog = false;
            ElMessage.success('合同 ' + c.contractNo + ' 已生成（待签约）');
            this.loadContracts();
        },
        async previewContract(row) {
            const d = await this.get('/api/contracts/' + row.id + '/content');
            this.contractContent = d.content;
            this.contentDialog = true;
        },
        async afterSign() {
            ElMessage.success('电子签约成功，房源已转为已出租，账单已生成推送');
            await Promise.all([this.loadContracts(), this.loadHouses(), this.loadBills(), this.loadNotif()]);
        },
        openTerminateDialog(row) {
            this.terminateRow = row;
            this.depositHandle = 'REFUNDED';
            this.terminateDialog = true;
        },
        async doTerminate() {
            await this.put(`/api/contracts/${this.terminateRow.id}/terminate?depositHandle=${this.depositHandle}`);
            this.terminateDialog = false;
            ElMessage.success('已办理退租，未缴租金账单作废，押金处理完成');
            await Promise.all([this.loadContracts(), this.loadHouses(), this.loadBills(), this.loadNotif()]);
        },

        /* ============ 费用管理 ============ */
        async loadBills() {
            const q = this.billQuery;
            this.bills = await this.get(`/api/bills?status=${q.status || ''}&type=${q.type || ''}`);
            if (!this.contracts.length) this.loadContracts();
        },
        openBillDialog() {
            this.billForm = { contractId: null, tenantId: null, tenantName: '', type: 'WATER',
                amount: 100, dueDate: this.plusDays(10), remark: '' };
            this.billDialog = true;
        },
        onBillContractChange(id) {
            const c = this.contracts.find(x => x.id === id);
            if (c) { this.billForm.tenantId = c.tenantId; this.billForm.tenantName = c.tenantName; }
        },
        async saveBill() {
            const f = this.billForm;
            if (!f.tenantName) return ElMessage.warning('请选择关联合同');
            await this.post('/api/bills', f);
            this.billDialog = false;
            ElMessage.success('账单已生成并推送给租客');
            this.loadBills();
            this.loadNotif();
        },
        async afterPay() {
            ElMessage.success('收费登记成功');
            await Promise.all([this.loadBills(), this.loadStats(), this.loadNotif()]);
        },

        /* ============ 工具方法 ============ */
        today() { return new Date().toISOString().slice(0, 10); },
        plusMonths(m) {
            const d = new Date(); d.setMonth(d.getMonth() + m);
            return d.toISOString().slice(0, 10);
        },
        plusDays(n) {
            const d = new Date(); d.setDate(d.getDate() + n);
            return d.toISOString().slice(0, 10);
        },
        fmtTime(t) { return t ? t.replace('T', ' ').slice(0, 16) : '-' },
        roleLabel(r) { return ROLE_LABELS[r] || r; },
        houseStatusLabel(s) { return { ON: '空置可租', RENTED: '已出租', OFF: '已下架' }[s] || s; },
        contractStatusLabel(s) { return { DRAFT: '待签约', SIGNED: '生效中', EXPIRED: '已到期', TERMINATED: '已退租' }[s] || s; },
        cycleLabel(c) { return { 1: '月付', 3: '季付', 6: '半年付', 12: '年付' }[c] || (c + '月'); },
        billTypeLabel(t) { return { RENT: '租金', DEPOSIT: '押金', WATER: '水费', ELECTRIC: '电费', PROPERTY: '物业费' }[t] || '其他'; },
        billStatusLabel(s) { return { UNPAID: '待缴费', PAID: '已缴费', CANCELLED: '已作废' }[s] || s; },
        depositLabel(h) { return { REFUNDED: '已退还', DEDUCTED: '已扣除', FORFEIT: '抵违约金' }[h] || '-'; },
        creditColor(score) { return score >= 90 ? '#67C23A' : score >= 75 ? '#409EFF' : score >= 60 ? '#E6A23C' : '#F56C6C'; },
        creditTag(level) { return { '优秀': 'success', '良好': 'primary', '一般': 'warning', '较差': 'danger' }[level] || 'info'; }
    }
}).use(ElementPlus, { locale: ElementPlusLocaleZhCn }).mount('#app');
